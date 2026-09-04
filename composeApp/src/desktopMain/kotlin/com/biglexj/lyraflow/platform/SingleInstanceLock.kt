package com.biglexj.lyraflow.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Garantía de Instancia Única y Despacho en Caliente (Single Instance Dispatch Lock).
 * Conforme a Core-Docs: `features/single-instance/README.md` y `platforms/windows/single_instance_lock.md`.
 *
 * Características:
 * - Aislamiento por canales: `stable` (puerto 49281) y `dev` (puerto 49283).
 * - En producción (`stable`) aplica exclusividad estricta de una sola instancia viva.
 * - Bypass garantizado en modo desarrollo (`isDevMode()`): no finaliza ni bloquea el proceso.
 * - Despacho IPC loopback con payload ("ACTIVATE" o argumentos) y cola resiliente.
 * - Soporte de liberación explícita (`release()`) para salidas limpias y auto-actualizaciones in-app.
 */
object SingleInstanceLock {
    const val STABLE_PORT = 49281
    const val DEV_PORT = 49283

    private var serverSocket: ServerSocket? = null

    @Volatile
    private var activationListener: ((String) -> Unit)? = null
    private val pendingPayloads = ConcurrentLinkedQueue<String>()

    fun getChannel(): String {
        return System.getProperty("lyraflow.channel")
            ?: if (System.getProperty("lyraflow.dev") == "true") "dev" else "stable"
    }

    fun isDevMode(): Boolean {
        return getChannel() == "dev" ||
            System.getProperty("lyraflow.dev") == "true" ||
            System.getProperty("idea.active") != null
    }

    fun allowsMultipleInstances(): Boolean {
        return System.getProperty("lyraflow.allowMultipleInstances") == "true"
    }

    fun getPortForChannel(channel: String = getChannel()): Int {
        System.getProperty("lyraflow.lock.port")?.toIntOrNull()?.let { return it }
        return when (channel.lowercase()) {
            "dev" -> DEV_PORT
            "test" -> 49285
            else -> STABLE_PORT
        }
    }

    /**
     * Registra o actualiza el listener que reacciona a órdenes de activación.
     * Si existían órdenes pendientes en cola recibidas durante el arranque, se despachan de inmediato.
     */
    fun registerActivationListener(listener: (String) -> Unit) {
        activationListener = listener
        while (true) {
            val pending = pendingPayloads.poll() ?: break
            SwingUtilities.invokeLater {
                listener.invoke(pending)
            }
        }
    }

    private fun dispatchPayload(payload: String) {
        val listener = activationListener
        if (listener != null) {
            SwingUtilities.invokeLater {
                listener.invoke(payload)
            }
        } else {
            pendingPayloads.offer(payload)
        }
    }

    /**
     * Intenta adquirir la exclusividad del canal o transfiere el comando a la instancia existente.
     * Retorna true si este proceso es la instancia primaria o está en dev; false si es secundaria en producción.
     */
    fun acquireOrTransfer(
        args: Array<String> = emptyArray(),
        onPayloadReceived: ((String) -> Unit)? = null,
    ): Boolean {
        if (onPayloadReceived != null) {
            registerActivationListener(onPayloadReceived)
        }

        if (allowsMultipleInstances()) {
            return true
        }

        val loopback = InetAddress.getByName("127.0.0.1")

        if (isDevMode()) {
            println("[SingleInstanceLock] Modo desarrollo activo. Bypass de bloqueo de instancia única permitido.")
            runCatching {
                val devPort = getPortForChannel("dev")
                val socket = ServerSocket(devPort, 10, loopback)
                serverSocket = socket
                startListenerThread(socket)
            }.onFailure {
                val devPort = getPortForChannel("dev")
                val payload = extractPayload(args)
                transferToExistingInstance(devPort, loopback, payload)
            }
            return true
        }

        val port = getPortForChannel(getChannel())

        return try {
            val socket = ServerSocket(port, 50, loopback)
            serverSocket = socket
            startListenerThread(socket)
            true
        } catch (_: Exception) {
            // Puerto ocupado por la instancia primaria en producción -> transferir orden y terminar
            val payload = extractPayload(args)
            transferToExistingInstance(port, loopback, payload)
            false
        }
    }

    private fun startListenerThread(socket: ServerSocket) {
        thread(isDaemon = true, name = "LyraFlow-SingleInstanceListener") {
            while (!socket.isClosed) {
                runCatching {
                    val client = socket.accept()
                    client.use { s ->
                        s.soTimeout = 2000
                        val line = s.getInputStream().bufferedReader().readLine()
                        val payload = if (line.isNullOrBlank()) "ACTIVATE" else line
                        dispatchPayload(payload)
                    }
                }
            }
        }
    }

    private fun extractPayload(args: Array<String>): String {
        if (args.isEmpty()) return "ACTIVATE"
        return args.joinToString(" ")
    }

    private fun transferToExistingInstance(port: Int, loopback: InetAddress, payload: String) {
        runCatching {
            Socket(loopback, port).use { socket ->
                socket.soTimeout = 2500
                val writer = socket.getOutputStream().bufferedWriter()
                writer.write(payload + "\n")
                writer.flush()
            }
        }
    }

    /**
     * Libera el socket y limpia el listener. Obligatorio al cerrar la app o antes de auto-actualizar.
     */
    fun release() {
        runCatching {
            serverSocket?.close()
            serverSocket = null
            activationListener = null
            pendingPayloads.clear()
        }
    }
}

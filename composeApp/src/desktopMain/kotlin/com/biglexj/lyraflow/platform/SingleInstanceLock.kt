package com.biglexj.lyraflow.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Garantía de Instancia Única y Despacho en Caliente (Single Instance Dispatch Lock).
 * Conforme a Core-Docs: `features/single-instance/README.md` y `platforms/windows/single_instance_lock.md`.
 *
 * Características:
 * - Aislamiento por canales: `stable` (puerto 49281) y `dev` (puerto 49283).
 * - En ambos canales aplica instancia única por defecto.
 * - Despacho de activación IPC local con payload ("ACTIVATE" o argumentos).
 * - Bypass explícito únicamente mediante `-Dlyraflow.allowMultipleInstances=true`.
 */
object SingleInstanceLock {
    const val STABLE_PORT = 49281
    const val DEV_PORT = 49283

    private var serverSocket: ServerSocket? = null

    fun getChannel(): String {
        return System.getProperty("lyraflow.channel")
            ?: if (System.getProperty("lyraflow.dev") == "true") "dev" else "stable"
    }

    fun isDevMode(): Boolean = getChannel() == "dev"

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
     * Intenta adquirir la exclusividad del canal o transfiere el comando a la instancia existente.
     * Retorna true si este proceso es la instancia primaria; false si es secundaria y debe cerrarse.
     */
    fun acquireOrTransfer(
        args: Array<String> = emptyArray(),
        onPayloadReceived: ((String) -> Unit)? = null,
    ): Boolean {
        if (allowsMultipleInstances()) {
            return true
        }

        val port = getPortForChannel()
        val loopback = InetAddress.getByName("127.0.0.1")

        return try {
            val socket = ServerSocket(port, 50, loopback)
            serverSocket = socket

            thread(isDaemon = true, name = "LyraFlow-SingleInstanceListener") {
                while (!socket.isClosed) {
                    runCatching {
                        val client = socket.accept()
                        client.use { s ->
                            s.soTimeout = 2000
                            val line = s.getInputStream().bufferedReader().readLine()
                            val payload = if (line.isNullOrBlank()) "ACTIVATE" else line
                            SwingUtilities.invokeLater {
                                onPayloadReceived?.invoke(payload)
                            }
                        }
                    }
                }
            }
            true
        } catch (_: Exception) {
            // El puerto está ocupado por la instancia primaria -> despachar orden y salir
            val payload = extractPayload(args)
            transferToExistingInstance(port, loopback, payload)
            false
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

    fun release() {
        runCatching {
            serverSocket?.close()
            serverSocket = null
        }
    }
}

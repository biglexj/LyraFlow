package com.biglexj.lyraflow.platform.hotkey

import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import kotlin.concurrent.thread

/**
 * Registra hasta dos combinaciones globales en paralelo: la principal para
 * alternar grabar/detener y, opcionalmente, la de cancelar un dictado en curso.
 * Cada combinacion necesita su propio identificador de hotkey en Win32.
 */
class WindowsGlobalShortcut : GlobalShortcut {
    private var worker: Thread? = null
    @Volatile private var threadId: Int = 0
    @Volatile private var registeredIds: List<Int> = emptyList()

    override var status: String = "Pendiente"
        private set

    override fun start(
        shortcut: KeyboardShortcut,
        onActivated: () -> Unit,
        cancelShortcut: KeyboardShortcut?,
        onCancel: () -> Unit,
    ) {
        if (worker != null) return
        worker = thread(name = "lyraflow-hotkey", isDaemon = true) {
            threadId = Kernel32.INSTANCE.GetCurrentThreadId()

            val bindings = buildList {
                add(Binding(PRIMARY_HOTKEY_ID, shortcut, onActivated))
                if (cancelShortcut != null) add(Binding(CANCEL_HOTKEY_ID, cancelShortcut, onCancel))
            }

            val failures = bindings.filterNot { binding ->
                User32.INSTANCE.RegisterHotKey(null, binding.id, modifiersOf(binding.shortcut), binding.shortcut.key.windowsCode)
            }
            registeredIds = bindings.map { it.id }

            status = buildString {
                val active = bindings.filter { it.id !in failures.map { f -> f.id } }.map { it.shortcut.label }
                append(active.joinToString(" / ").ifBlank { "Sin atajos" })
                if (failures.isNotEmpty()) {
                    append(" · no disponible: ")
                    append(failures.joinToString(", ") { "${it.shortcut.label}" })
                }
                append(if (failures.isEmpty()) " activo" else "")
            }

            if (registeredIds.isEmpty()) return@thread

            val message = WinUser.MSG()
            while (User32.INSTANCE.GetMessage(message, null, 0, 0) > 0) {
                if (message.message != WinUser.WM_HOTKEY) continue
                when (message.wParam.toInt()) {
                    PRIMARY_HOTKEY_ID -> onActivated()
                    CANCEL_HOTKEY_ID -> onCancel()
                }
            }
            unregisterAll()
        }
    }

    private fun unregisterAll() {
        registeredIds.forEach { id ->
            runCatching { User32.INSTANCE.UnregisterHotKey(null, id) }
        }
        registeredIds = emptyList()
    }

    override fun close() {
        val current = worker ?: return
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WinUser.WM_QUIT, WPARAM(0), LPARAM(0))
        }
        current.join(1_000)
        current.interrupt()
        worker = null
        threadId = 0
    }

    private fun modifiersOf(shortcut: KeyboardShortcut): Int =
        shortcut.modifiers.fold(WinUser.MOD_NOREPEAT) { flags, modifier ->
            flags or modifier.windowsFlag
        }

    private data class Binding(
        val id: Int,
        val shortcut: KeyboardShortcut,
        val action: () -> Unit,
    )

    private companion object {
        const val PRIMARY_HOTKEY_ID = 0x4C59
        const val CANCEL_HOTKEY_ID = 0x4C5A
    }
}

package com.biglexj.lyraflow.core.update

import com.biglexj.lyraflow.platform.SingleInstanceLock
import java.io.File
import kotlin.system.exitProcess

actual fun openInstaller(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return@runCatching

        // 1. Liberar el Single-Instance Lock para permitir que el instalador o la nueva versión tome el socket
        SingleInstanceLock.release()

        // 2. Determinar la ruta del ejecutable instalado para relanzarlo tras la actualización
        val currentExePath = ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { File(it).exists() }
            ?: File(System.getenv("LOCALAPPDATA") ?: ".", "LyraFlow/LyraFlow.exe").absolutePath

        val absPath = file.absolutePath

        // 3. Comando cmd desasociado: esperar 2s para liberar archivos en uso, ejecutar instalador EXE con /passive y relanzar LyraFlow
        val installCmd = "timeout /t 2 /nobreak > nul & start /wait \"\" \"$absPath\" /passive & start \"\" \"$currentExePath\""

        ProcessBuilder("cmd.exe", "/c", installCmd).start()

        // 4. Finalizar inmediatamente la instancia antigua
        exitProcess(0)
    }.onFailure {
        it.printStackTrace()
    }
}

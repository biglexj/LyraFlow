package com.biglexj.lyraflow.core.update

import com.biglexj.lyraflow.platform.SingleInstanceLock
import java.io.File
import kotlin.system.exitProcess

actual fun openInstaller(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return@runCatching

        // Inno conoce la ruta instalada y solo relanza la app si termina correctamente.
        // Argumentos separados: las rutas no pasan por cmd ni se interpretan como comandos.
        ProcessBuilder(innoUpdateCommand(file)).start()
        SingleInstanceLock.release()
        exitProcess(0)
    }.onFailure {
        it.printStackTrace()
    }
}

internal fun innoUpdateCommand(file: File): List<String> {
    require(file.isFile && file.extension.equals("exe", ignoreCase = true)) { "No se encontró el instalador EXE." }
    return listOf(file.absolutePath, "/SILENT", "/NORESTART", "/SP-", "/RESTARTAPP", "/LOG")
}

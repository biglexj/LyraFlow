package com.biglexj.lyraflow.core.update

import java.io.File
import kotlin.system.exitProcess

actual fun openInstaller(filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return@runCatching

        if (file.name.endsWith(".msi", ignoreCase = true)) {
            ProcessBuilder("msiexec", "/i", file.absolutePath, "/passive").start()
        } else {
            ProcessBuilder(file.absolutePath).start()
        }
        exitProcess(0)
    }.onFailure {
        it.printStackTrace()
    }
}

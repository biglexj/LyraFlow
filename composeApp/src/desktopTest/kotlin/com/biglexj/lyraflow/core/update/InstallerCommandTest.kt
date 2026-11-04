package com.biglexj.lyraflow.core.update

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InstallerCommandTest {
    @Test fun treatsShellCharactersAndSpacesAsOnePathArgument() {
        val dir = Files.createTempDirectory("Lyra update & espacio")
        val exe = Files.createFile(dir.resolve("LyraFlow.exe")).toFile()
        try {
            val args = innoUpdateCommand(exe)
            assertEquals(exe.absolutePath, args.first())
            assertTrue(args.containsAll(listOf("/SILENT", "/NORESTART", "/RESTARTAPP")))
        } finally { exe.delete(); Files.delete(dir) }
    }

    @Test fun rejectsMsiAndMissingFiles() {
        val msi = Files.createTempFile("Lyra", ".msi").toFile()
        try { assertFailsWith<IllegalArgumentException> { innoUpdateCommand(msi) } }
        finally { msi.delete() }
        assertFailsWith<IllegalArgumentException> { innoUpdateCommand(msi) }
    }
}

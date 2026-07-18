package com.biglexj.lyraflow.platform.settings

import com.sun.jna.platform.win32.Crypt32Util
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.prefs.Preferences

class DesktopApiKeyStore(
    private val node: Preferences = Preferences.userRoot().node(NODE),
    private val protector: ApiKeyProtector = platformProtector(),
) {
    fun load(): String = runCatching {
        node.get(API_KEY, null)?.let(protector::unprotect).orEmpty()
    }.getOrDefault("")

    fun save(value: String) {
        if (value.isBlank()) node.remove(API_KEY) else node.put(API_KEY, protector.protect(value.trim()))
        node.flush()
    }

    private companion object {
        const val NODE = "com/biglexj/lyraflow"
        const val API_KEY = "geminiApiKey.protected"

        fun platformProtector(): ApiKeyProtector =
            if (System.getProperty("os.name").contains("windows", ignoreCase = true)) DpapiApiKeyProtector()
            else PortableApiKeyProtector()
    }
}

interface ApiKeyProtector {
    fun protect(value: String): String
    fun unprotect(value: String): String
}

class DpapiApiKeyProtector : ApiKeyProtector {
    override fun protect(value: String): String = Base64.getEncoder().encodeToString(
        Crypt32Util.cryptProtectData(value.toByteArray(StandardCharsets.UTF_8)),
    )

    override fun unprotect(value: String): String = String(
        Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(value)),
        StandardCharsets.UTF_8,
    )
}

private class PortableApiKeyProtector : ApiKeyProtector {
    override fun protect(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray())
    override fun unprotect(value: String): String = String(Base64.getDecoder().decode(value))
}

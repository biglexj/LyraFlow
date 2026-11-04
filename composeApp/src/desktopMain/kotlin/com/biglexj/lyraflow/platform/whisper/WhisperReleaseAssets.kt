package com.biglexj.lyraflow.platform.whisper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * Resuelve el paquete de binarios de whisper.cpp para Windows a partir del listado de releases.
 *
 * Upstream publica los binarios de forma irregular: algunas releases estables (por ejemplo v1.9.3,
 * v1.9.4 o v1.9.5) se publican sin adjuntos y solo las builds `b####` incluyen `whisper-bin-x64.zip`.
 * Por eso no sirve con mirar `/releases/latest`: hay que recorrer el listado completo y quedarse con
 * la release más reciente que sí traiga el paquete.
 */
internal object WhisperReleaseAssets {
    const val WINDOWS_PACKAGE = "whisper-bin-x64.zip"

    /**
     * Devuelve la URL de descarga del paquete Windows o `null` si ninguna release lo incluye.
     * Ante un JSON inesperado devuelve `null` en lugar de propagar la excepción.
     */
    fun selectWindowsPackage(releasesJson: String): String? {
        val releases = runCatching { Json.parseToJsonElement(releasesJson).jsonArray }.getOrNull() ?: return null
        for (release in releases) {
            val assets = (release as? JsonObject)?.get("assets") as? JsonArray ?: continue
            for (asset in assets) {
                val assetObject = asset as? JsonObject ?: continue
                if ((assetObject["name"] as? JsonPrimitive)?.content != WINDOWS_PACKAGE) continue
                val url = (assetObject["browser_download_url"] as? JsonPrimitive)?.content
                if (!url.isNullOrBlank()) return url
            }
        }
        return null
    }
}

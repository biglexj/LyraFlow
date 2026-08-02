package com.biglexj.lyraflow.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.update.UpdateRelease

/** Elimina sintaxis Markdown básica y saltos de línea para mostrar texto limpio en la UI. */
private fun sanitizeMarkdown(text: String): String =
    text
        .replace(Regex("\\*{1,3}(.+?)\\*{1,3}"), "$1")  // negrita / cursiva
        .replace(Regex("#{1,6}\\s*"), "")                 // encabezados
        .replace(Regex("`{1,3}[^`]*`{1,3}"), "")           // código inline / bloque
        .replace(Regex("-\\s+"), "• ")                     // listas
        .replace(Regex("\\r\\n|\\n\\r|\\r"), " ")         // saltos de línea (CR/CRLF)
        .replace("\n", " ")                               // saltos de línea (LF)
        .replace(Regex(" {2,}"), " ")                     // espacios múltiples
        .trim()

@Composable
fun UpdateBanner(
    release: UpdateRelease,
    downloadProgress: Float? = null,
    downloadMb: String? = null,
    totalMb: String? = null,
    isReadyToInstall: Boolean = false,
    onStartDownload: (() -> Unit)? = null,
    onInstallAndRestart: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val bodyText = release.body
        .let { sanitizeMarkdown(it) }
        .ifBlank { "Hay una versión más reciente de LyraFlow con mejoras de rendimiento y correcciones." }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when {
                        isReadyToInstall -> "🎉 ¡Actualización v${release.version} lista para instalar!"
                        downloadProgress != null -> "📥 Descargando actualización v${release.version}..."
                        else -> "🚀 ¡Nueva actualización disponible: v${release.version}!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        isReadyToInstall -> "La descarga ha finalizado exitosamente. Haz clic en 'Instalar y Reiniciar' para actualizar en caliente sin perder tus ajustes."
                        downloadProgress != null && downloadMb != null -> "Progreso: $downloadMb MB / $totalMb MB (${(downloadProgress * 100).toInt()}%)"
                        else -> bodyText
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (downloadProgress == null && !isReadyToInstall) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Ahora no")
                    }
                }

                when {
                    isReadyToInstall && onInstallAndRestart != null -> {
                        Button(onClick = onInstallAndRestart) {
                            Text("Instalar y Reiniciar 🚀")
                        }
                    }
                    downloadProgress != null -> {
                        Text(
                            text = "Descargando en segundo plano...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    onStartDownload != null -> {
                        Button(onClick = onStartDownload) {
                            Text("Actualizar ahora")
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                val targetUrl = release.downloadUrl.ifBlank { release.releasePageUrl }
                                uriHandler.openUri(targetUrl)
                                onDismiss()
                            },
                        ) {
                            Text("Actualizar ahora")
                        }
                    }
                }
            }
        }
    }
}

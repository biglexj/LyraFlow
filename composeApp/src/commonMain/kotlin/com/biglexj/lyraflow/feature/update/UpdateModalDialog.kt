package com.biglexj.lyraflow.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.biglexj.lyraflow.core.update.UpdateRelease

/** Sanitización canónica según regla auto_updater.md */
fun sanitizeReleaseNotes(text: String): String =
    text
        .replace(Regex("\\*{1,3}(.+?)\\*{1,3}"), "$1")  // negrita / cursiva
        .replace(Regex("#{1,6}\\s*"), "")                 // encabezados
        .replace(Regex("`{1,3}[^`]*`{1,3}"), "")           // código inline / bloque
        .replace(Regex("-\\s+"), "• ")                     // listas
        .replace(Regex("\\r\\n|\\n\\r|\\r"), "\n")        // normalización de saltos
        .replace(Regex(" {2,}"), " ")                     // espacios múltiples
        .trim()

@Composable
fun UpdateModalDialog(
    release: UpdateRelease,
    downloadProgress: Float? = null,
    downloadMb: String? = null,
    totalMb: String? = null,
    isReadyToInstall: Boolean = false,
    onStartDownload: (() -> Unit)? = null,
    onInstallAndRestart: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val cleanNotes = sanitizeReleaseNotes(release.body)
        .ifBlank { "Hay una versión más reciente de LyraFlow con mejoras de rendimiento y correcciones." }

    Dialog(
        onDismissRequest = {
            if (downloadProgress == null && !isReadyToInstall) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .widthIn(max = 480.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when {
                            isReadyToInstall -> "🎉 ¡Actualización v${release.version} lista!"
                            downloadProgress != null -> "📥 Descargando v${release.version}..."
                            else -> "🚀 Actualización Disponible v${release.version}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Notas de la versión:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = cleanNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (downloadProgress != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (downloadMb != null && totalMb != null) {
                                "Progreso: $downloadMb MB / $totalMb MB (${(downloadProgress * 100).toInt()}%)"
                            } else {
                                "Descargando... (${(downloadProgress * 100).toInt()}%)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                                text = "Descargando...",
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
}

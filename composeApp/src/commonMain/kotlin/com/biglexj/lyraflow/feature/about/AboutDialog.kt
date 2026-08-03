package com.biglexj.lyraflow.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.update.UpdateRelease

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    isCheckingUpdates: Boolean = false,
    upToDateStatus: Boolean = false,
    availableUpdate: UpdateRelease? = null,
    onOpenUpdateModal: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "LyraFlow",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "v1.1.3 • MIT License",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Desarrollado por biglexj (2026)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Dictado por voz fluido e inteligente integrado con IA multimodal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Si esta herramienta agiliza tu flujo de trabajo diario, considera apoyar su desarrollo continuo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { uriHandler.openUri("https://www.biglexj.com/donaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Donaciones Oficiales (Yape / Plin / Web)")
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://buymeacoffee.com/biglexj") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Buy Me a Coffee ☕")
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/biglexj/LyraFlow/issues") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Enviar Feedback / Reportar Error 💬")
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/biglexj") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Perfil de GitHub")
                    }
                }

                // Zona de Estado de Actualización en la parte inferior del modal
                when {
                    isCheckingUpdates -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "  Buscando actualizaciones...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    availableUpdate != null -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "🚀 ¡Nueva v${availableUpdate.version} disponible!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Haz clic para ver las mejoras e instalar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    )
                                }
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenUpdateModal()
                                    },
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text("Ver")
                                }
                            }
                        }
                    }
                    upToDateStatus -> {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                text = "✅ Estás en la última versión de LyraFlow (v1.1.3).",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdates,
                ) {
                    Text("🔄 Buscar actualizaciones")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
    )
}

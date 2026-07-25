package com.biglexj.lyraflow.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                    text = "🚀 ¡Nueva actualización disponible: v${release.version}!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text("Ahora no")
                }
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

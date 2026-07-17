package com.biglexj.lyraflow.feature.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.biglexj.lyraflow.core.hotkey.ShortcutKey
import com.biglexj.lyraflow.core.hotkey.ShortcutModifier

@Composable
fun ShortcutRecorder(shortcut: KeyboardShortcut, onShortcutChange: (KeyboardShortcut) -> Unit) {
    var capturing by remember { mutableStateOf(false) }
    var message by remember(shortcut) { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(capturing) {
        if (capturing) focusRequester.requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Atajo actual", style = MaterialTheme.typography.bodyMedium)
                    Text(shortcut.label, style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = { capturing = true; message = null },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (!capturing || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            val candidate = event.toShortcut()
                            val error = candidate?.validationError()
                            if (candidate != null && error == null) {
                                onShortcutChange(candidate)
                                capturing = false
                                message = "Atajo actualizado."
                            } else {
                                message = error
                                    ?: "Mantén uno o dos modificadores y pulsa una tecla principal."
                            }
                            true
                        },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(if (capturing) "Pulsa las teclas…" else "Cambiar")
                }
            }
        }
        Text(
            message ?: "Usa una combinación de 2 o 3 teclas. F12 y Windows + Espacio están reservados.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (message?.contains("actualizado") == true) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private fun KeyEvent.toShortcut(): KeyboardShortcut? {
    val mainKey = key.toShortcutKey() ?: return null
    val modifiers = buildSet {
        if (isCtrlPressed) add(ShortcutModifier.Control)
        if (isAltPressed) add(ShortcutModifier.Alt)
        if (isShiftPressed) add(ShortcutModifier.Shift)
        if (isMetaPressed) add(ShortcutModifier.Windows)
    }
    return KeyboardShortcut(modifiers, mainKey)
}

private fun Key.toShortcutKey(): ShortcutKey? = when (this) {
    Key.Spacebar -> ShortcutKey.Space
    Key.A -> ShortcutKey.A
    Key.B -> ShortcutKey.B
    Key.C -> ShortcutKey.C
    Key.D -> ShortcutKey.D
    Key.E -> ShortcutKey.E
    Key.F -> ShortcutKey.F
    Key.G -> ShortcutKey.G
    Key.H -> ShortcutKey.H
    Key.I -> ShortcutKey.I
    Key.J -> ShortcutKey.J
    Key.K -> ShortcutKey.K
    Key.L -> ShortcutKey.L
    Key.M -> ShortcutKey.M
    Key.N -> ShortcutKey.N
    Key.O -> ShortcutKey.O
    Key.P -> ShortcutKey.P
    Key.Q -> ShortcutKey.Q
    Key.R -> ShortcutKey.R
    Key.S -> ShortcutKey.S
    Key.T -> ShortcutKey.T
    Key.U -> ShortcutKey.U
    Key.V -> ShortcutKey.V
    Key.W -> ShortcutKey.W
    Key.X -> ShortcutKey.X
    Key.Y -> ShortcutKey.Y
    Key.Z -> ShortcutKey.Z
    Key.F1 -> ShortcutKey.F1
    Key.F2 -> ShortcutKey.F2
    Key.F3 -> ShortcutKey.F3
    Key.F4 -> ShortcutKey.F4
    Key.F5 -> ShortcutKey.F5
    Key.F6 -> ShortcutKey.F6
    Key.F7 -> ShortcutKey.F7
    Key.F8 -> ShortcutKey.F8
    Key.F9 -> ShortcutKey.F9
    Key.F10 -> ShortcutKey.F10
    Key.F11 -> ShortcutKey.F11
    else -> null
}

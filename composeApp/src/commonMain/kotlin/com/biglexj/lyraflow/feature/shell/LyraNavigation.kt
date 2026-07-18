package com.biglexj.lyraflow.feature.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.feature.components.LyraIcon
import com.biglexj.lyraflow.feature.components.LyraIconType

@Composable
fun LyraNavigationRail(
    selected: AppDestination,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onSelect: (AppDestination) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxHeight().width(92.dp).padding(horizontal = 10.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onCycleTheme,
                modifier = Modifier.width(RailItemWidth).height(52.dp).semantics {
                    contentDescription = "Tema ${themeMode.label}. Cambiar tema."
                },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                LyraIcon(themeMode.icon, Modifier.padding(12.dp))
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.width(RailItemWidth),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppDestination.entries.forEach { destination ->
                    DestinationButton(
                        destination,
                        selected == destination,
                        onSelect,
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

private val RailItemWidth = 64.dp

private val ThemeMode.icon: LyraIconType
    get() = when (this) {
        ThemeMode.System -> LyraIconType.System
        ThemeMode.Light -> LyraIconType.Sun
        ThemeMode.Dark -> LyraIconType.Moon
    }

@Composable
fun LyraNavigationBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppDestination.entries.forEach { destination ->
                DestinationButton(destination, selected == destination, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DestinationButton(
    destination: AppDestination,
    selected: Boolean,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onSelect(destination) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LyraIcon(destination.icon, Modifier.size(22.dp))
            Text(destination.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

package com.biglexj.lyraflow.feature.shell

import com.biglexj.lyraflow.feature.components.LyraIconType

enum class AppDestination(
    val label: String,
    val icon: LyraIconType,
) {
    Home("Inicio", LyraIconType.Home),
    History("Historial", LyraIconType.History),
    Settings("Ajustes", LyraIconType.Settings),
}

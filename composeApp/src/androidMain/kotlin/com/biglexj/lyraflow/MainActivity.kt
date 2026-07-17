package com.biglexj.lyraflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.domain.dictation.DictationState
import com.biglexj.lyraflow.feature.shell.LyraFlowApp
import com.biglexj.lyraflow.feature.shell.ShellActions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var preferences by remember { mutableStateOf(AppPreferences()) }
            var apiKey by remember { mutableStateOf("") }
            LyraFlowApp(
                platform = "Android · prototipo de UI compartida",
                state = DictationState.Idle,
                configuration = AppConfiguration(preferences, apiKey),
                whisperStatus = "fuera del MVP Android de la Fase 0",
                actions = ShellActions(
                    toggleRecording = {},
                    injectLastResult = {},
                    reset = {},
                    updatePreferences = { preferences = it },
                    updateApiKey = { apiKey = it },
                ),
            )
        }
    }
}

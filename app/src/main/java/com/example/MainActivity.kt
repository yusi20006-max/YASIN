package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.firebase.configureAppCheck
import com.example.data.repository.RelaySettings
import com.example.di.AppContainer
import com.example.ui.navigation.YasinNavApp
import com.example.ui.theme.YasinHubTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required: Configure App Check with launch intent before UI, ViewModels, or Firebase AI calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        appContainer = AppContainer(applicationContext)

        setContent {
            val settings by appContainer.relayConfigRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = RelaySettings())

            YasinHubTheme(darkTheme = settings.darkTheme) {
                YasinNavApp(appContainer = appContainer)
            }
        }
    }
}

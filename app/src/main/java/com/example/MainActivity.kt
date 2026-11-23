package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.PdfScanNavHost
import com.example.ui.theme.PdfScanTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PdfScanApplication).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(container))
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            PdfScanTheme(darkTheme = settings.darkThemeEnabled) {
                PdfScanNavHost(
                    container = container,
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

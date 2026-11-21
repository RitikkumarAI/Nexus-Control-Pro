package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LiveAlertBanner
import com.example.ui.navigation.NexusNavGraph
import com.example.ui.theme.NexusThemeProvider
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val themeModeStr by mainViewModel.themeModeState.collectAsState()

            val currentThemeMode = when (themeModeStr.uppercase()) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            NexusThemeProvider(
                themeMode = currentThemeMode,
                onThemeModeChanged = { newMode ->
                    mainViewModel.setThemeMode(newMode.name)
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NexusNavGraph(mainViewModel = mainViewModel)
                        LiveAlertBanner(modifier = Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}

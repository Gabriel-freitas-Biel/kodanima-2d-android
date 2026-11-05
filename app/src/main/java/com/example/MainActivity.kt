package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.model.AppScreen
import com.example.model.AppUiMode
import com.example.ui.screens.AnimationEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PcEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.KodanimaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KodanimaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initializeWithContext(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(kodanimaTheme = uiState.preferences.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (uiState.currentScreen) {
                        AppScreen.HOME -> {
                            HomeScreen(viewModel = viewModel)
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                        AppScreen.EDITOR -> {
                            when (uiState.preferences.uiMode) {
                                AppUiMode.PC -> {
                                    PcEditorScreen(viewModel = viewModel)
                                }
                                AppUiMode.MOBILE -> {
                                    AnimationEditorScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.handlePhysicalKeyEvent(event, this)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

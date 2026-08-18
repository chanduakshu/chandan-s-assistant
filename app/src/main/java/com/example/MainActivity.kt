package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CallDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveScreeningScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssistantViewModel

sealed interface Screen {
    data object Home : Screen
    data object Simulator : Screen
    data class CallDetail(val callId: Long) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    private val assistantViewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = assistantViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: AssistantViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onOpenCallDetail = { callId ->
                    currentScreen = Screen.CallDetail(callId)
                },
                onOpenCallSimulator = {
                    currentScreen = Screen.Simulator
                },
                onOpenSettings = {
                    currentScreen = Screen.Settings
                }
            )
        }

        is Screen.Simulator -> {
            LiveScreeningScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.dismissCallSimulator()
                    currentScreen = Screen.Home
                },
                onViewHistory = {
                    viewModel.dismissCallSimulator()
                    currentScreen = Screen.Home
                }
            )
        }

        is Screen.CallDetail -> {
            CallDetailScreen(
                callId = screen.callId,
                viewModel = viewModel,
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    currentScreen = Screen.Home
                }
            )
        }
    }
}

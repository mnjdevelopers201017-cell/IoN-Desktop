import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement

enum class Screen { SPLASH, AUTH, DASHBOARD, AUTOMATION }

fun main() = application {
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "IoN Desktop",
        state = windowState
    ) {
        MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF000000))) {
            when (currentScreen) {
                Screen.SPLASH -> SplashScreen(onFinished = { currentScreen = Screen.AUTH })
                Screen.AUTH -> AuthScreen(onLoginSuccess = { currentScreen = Screen.DASHBOARD })
                Screen.DASHBOARD -> DashboardScreen()
                Screen.AUTOMATION -> AutomationUI(windowState)
            }
        }
    }
}

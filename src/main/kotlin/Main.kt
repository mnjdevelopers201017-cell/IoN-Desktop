import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import kotlinx.coroutines.launch

fun main() = application {
    var isSplashFinished by remember { mutableStateOf(false) }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "IoN Desktop",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF000000))) {
            if (!isSplashFinished) {
                SplashScreen(onFinished = { isSplashFinished = true })
            } else {
                AutomationUI()
            }
        }
    }
}

@Composable
fun AutomationUI() {
    var apiKey by remember { mutableStateOf("") }
    var task by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("Ready to automate...") }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Column(
            modifier = Modifier.width(400.dp).fillMaxHeight().background(Color(0xFF1A1A1A)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("IoN Automation Engine", color = Color.White, fontSize = 24.sp)
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4285F4)
                )
            )

            OutlinedTextField(
                value = task,
                onValueChange = { task = it },
                label = { Text("What should the AI do?") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4285F4)
                )
            )

            Button(
                onClick = {
                    if (isRunning) {
                        AutomationEngine.stopTask()
                    } else {
                        if (apiKey.isBlank() || task.isBlank()) {
                            logText = "⚠️ Please enter API Key and Task."
                            return@Button
                        }
                        isRunning = true
                        logText = "Starting..."
                        scope.launch {
                            AutomationEngine.runTask(task, apiKey) { msg ->
                                logText += "\n$msg"
                            }
                            isRunning = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE53935) else Color(0xFF4285F4)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isRunning) "Stop Automation" else "Start Automation", color = Color.White)
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)
        ) {
            Text("Execution Log", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp)).padding(16.dp)
            ) {
                Text(
                    text = logText,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

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
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.launch

@Composable
fun AutomationUI(windowState: WindowState) {
    var apiKey by remember { mutableStateOf("") }
    var task by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("Ready to automate...") }
    var isRunning by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Permissions Guide Dialog
    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("How to enable Automation Permissions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("On Windows (Important):", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Windows blocks automation by default. Right-click the IoN app icon and select 'Run as Administrator'.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("On Linux (X11/Wayland):", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Ensure the app has access to /dev/uinput. Run: sudo usermod -aG input \$USER and restart the session.", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("On macOS:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Go to System Settings -> Privacy & Security -> Accessibility and add the IoN app.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuide = false }) { Text("Got it") }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Column(
            modifier = Modifier.width(400.dp).fillMaxHeight().background(Color(0xFF1A1A1A)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("IoN Automation Engine", color = Color.White, fontSize = 24.sp)
            
            // NEW: Permissions & Setup Help Button
            OutlinedButton(
                onClick = { showGuide = true },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Yellow),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("⚠️ Setup & Permissions Guide", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val result = AutomationEngine.testMouseControl()
                    logText += "\n[TEST] $result"
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Test Mouse Permissions", color = Color.White, fontSize = 12.sp)
            }

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
                modifier = Modifier.fillMaxWidth().height(120.dp),
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
                            logText += "\n⚠️ Please enter API Key and Task."
                            return@Button
                        }
                        isRunning = true
                        logText += "\n▶ Starting task: $task"
                        
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
                    fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

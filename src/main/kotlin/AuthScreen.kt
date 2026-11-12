import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import com.sun.net.httpserver.HttpServer

// The exact SVG paths converted to Compose ImageVector
val GoogleIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Google",
        defaultWidth = 32.dp,
        defaultHeight = 32.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).apply {
        path(fill = SolidColor(Color(0xFF00AC47))) {
            moveTo(23.75f, 16f)
            arcToRelative(7.7446f, 7.7446f, 0f, false, true, -15.0323f, 2.6259f)
            lineTo(4.2849f, 22.1721f)
            arcTo(13.244f, 13.244f, 0f, false, false, 29.25f, 16f)
            close()
        }
        path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(23.75f, 16f)
            arcToRelative(7.7387f, 7.7387f, 0f, false, true, -3.2516f, 6.2987f)
            lineToRelative(4.3824f, 3.5059f)
            arcTo(13.2042f, 13.2042f, 0f, false, false, 29.25f, 16f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFBA00))) {
            moveTo(8.25f, 16f)
            arcToRelative(7.698f, 7.698f, 0f, false, true, 0.4677f, -2.6259f)
            lineTo(4.2849f, 9.8279f)
            arcToRelative(13.177f, 13.177f, 0f, false, false, 0f, 12.3442f)
            lineToRelative(4.4328f, -3.5462f)
            arcTo(7.698f, 7.698f, 0f, false, true, 8.25f, 16f)
            close()
        }
        path(fill = SolidColor(Color(0xFFEA4435))) {
            moveTo(16f, 8.25f)
            arcToRelative(7.699f, 7.699f, 0f, false, true, 4.558f, 1.4958f)
            lineToRelative(4.06f, -3.7893f)
            arcTo(13.2152f, 13.2152f, 0f, false, false, 4.2849f, 9.8279f)
            lineToRelative(4.4328f, 3.5462f)
            arcTo(7.756f, 7.756f, 0f, false, true, 16f, 8.25f)
            close()
        }
        path(fill = SolidColor(Color(0xFF4285F4))) {
            moveTo(29.25f, 15f)
            verticalLineToRelative(1f)
            lineTo(27f, 19.5f)
            horizontalLineTo(16.5f)
            verticalLineTo(14f)
            horizontalLineTo(28.25f)
            arcTo(1f, 1f, 0f, false, true, 29.25f, 15f)
            close()
        }
    }.build()
}

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var startLogoAnim by remember { mutableStateOf(false) }
    var showUi by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val offsetTopBar by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 0))
    val offsetTopStem by animateIntAsState(if (startLogoAnim) 0 else 2000, tween(600, 100))
    val offsetCenter by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 200))
    val offsetBottomStem by animateIntAsState(if (startLogoAnim) 0 else 2000, tween(600, 300))
    val offsetBottomBar by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 400))

    val uiAlpha by animateFloatAsState(if (showUi) 1f else 0f, tween(800))
    val uiOffsetY by animateFloatAsState(if (showUi) 0f else -50f, tween(800))

    LaunchedEffect(Unit) {
        delay(300)
        startLogoAnim = true
        delay(1000)
        showUi = true
    }

    fun loginWithGoogle() {
        isLoading = true
        errorMessage = ""
        scope.launch {
            try {
                val server = HttpServer.create(InetSocketAddress(8080), 0)
                
                server.createContext("/") { exchange ->
                    val query = exchange.requestURI.query
                    val code = query?.split("&")?.find { it.startsWith("code=") }?.substringAfter("code=")
                    
                    if (code != null) {
                        val response = "<html><body style='background:#000;color:#fff;text-align:center;padding-top:50px;'><h2>Login Successful!</h2><p>You can close this tab and return to IoN.</p></body></html>"
                        exchange.sendResponseHeaders(200, response.length.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }
                        
                        scope.launch {
                            try {
                                Supabase.client.auth.exchangeCodeForSession(code)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                errorMessage = "Auth Error: ${e.message}"
                            } finally {
                                isLoading = false
                                server.stop(0)
                            }
                        }
                    } else {
                        exchange.sendResponseHeaders(400, -1)
                        errorMessage = "Login failed: No code received."
                        isLoading = false
                        server.stop(0)
                    }
                }
                
                server.start()

                val authUrl = Supabase.client.auth.getOAuthUrl(Google, "http://localhost:8080")
                
                // Tries to open browser. If it fails, tries xdg-open. If that fails, prints to terminal.
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI(authUrl))
                    } else {
                        ProcessBuilder("xdg-open", authUrl).start()
                        errorMessage = "Opening browser via xdg-open..."
                    }
                } catch (e: Exception) {
                    println("MANUAL LOGIN REQUIRED: Copy and paste this URL into your browser:\n$authUrl")
                    errorMessage = "Please copy the link from the terminal into Chrome."
                }

            } catch (e: Exception) {
                errorMessage = "Failed to start server: ${e.message}"
                isLoading = false
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val isWideScreen = maxWidth > 900.dp

        Row(Modifier.fillMaxSize()) {
            if (isWideScreen) {
                Box(Modifier.weight(1f).fillMaxHeight().alpha(uiAlpha).offset { IntOffset(0, uiOffsetY.toInt()) }) {
                    Image(
                        painter = painterResource("login_bg.png"), 
                        contentDescription = "bg", 
                        contentScale = ContentScale.Crop, 
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                    Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.Center) {
                        Text(
                            "Can't find anything? Can't do anything?\nOr want to turn your device into a source of income?\n\nThat's exactly why IoN is here.",
                            color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 45.sp
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    Box(Modifier.offset { IntOffset(offsetTopBar, 0) }.width(120.dp).height(25.dp).background(Color.White))
                    Box(Modifier.offset { IntOffset(offsetTopStem, 0) }.width(40.dp).height(35.dp).background(Color.White))
                    Box(Modifier.offset { IntOffset(offsetCenter, 0) }.width(100.dp).height(50.dp).background(Color.White), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(40.dp).height(30.dp).background(Color.Black))
                    }
                    Box(Modifier.offset { IntOffset(offsetBottomStem, 0) }.width(40.dp).height(35.dp).background(Color.White))
                    Box(Modifier.offset { IntOffset(offsetBottomBar, 0) }.width(120.dp).height(25.dp).background(Color.White))

                    Spacer(Modifier.height(16.dp))
                    Text("IoN", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(uiAlpha))
                    Spacer(Modifier.height(8.dp))
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(uiAlpha).offset { IntOffset(0, uiOffsetY.toInt()) }) {
                        Text("Simply the best AI automation system you will ever find", color = Color(0xFFB3B3B3), fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { loginWithGoogle() },
                            enabled = !isLoading,
                            modifier = Modifier.width(320.dp).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = GoogleIcon,
                                    contentDescription = "Google Logo",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified // Keep original SVG colors
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(if (isLoading) "Waiting..." else "Continue with Google", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("OR", color = Color(0xFFB3B3B3), fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.width(320.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedLabelColor = Color(0xFF4285F4),
                                unfocusedLabelColor = Color(0xFF777777)
                            )
                        )
                        
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(icon, contentDescription = "Toggle Password Visibility", tint = Color(0xFF777777))
                                }
                            },
                            modifier = Modifier.width(320.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedLabelColor = Color(0xFF4285F4),
                                unfocusedLabelColor = Color(0xFF777777)
                            )
                        )

                        Row(modifier = Modifier.width(320.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    if (email.isBlank()) {
                                        errorMessage = "Enter your email to reset password."
                                    } else {
                                        scope.launch {
                                            try {
                                                Supabase.client.auth.resetPasswordForEmail(email)
                                                errorMessage = "Password reset email sent!"
                                            } catch (e: Exception) {
                                                errorMessage = "Error: ${e.message}"
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Forgot Password?", color = Color(0xFF4285F4), fontSize = 12.sp)
                            }
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    try {
                                        Supabase.client.auth.signInWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        onLoginSuccess()
                                    } catch (e: Exception) {
                                        try {
                                            Supabase.client.auth.signUpWith(Email) {
                                                this.email = email
                                                this.password = password
                                            }
                                            onLoginSuccess()
                                        } catch (signUpError: Exception) {
                                            errorMessage = signUpError.message ?: "Authentication failed"
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.width(320.dp).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3B3B3)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isLoading) "CONNECTING..." else "SIGN IN / REGISTER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

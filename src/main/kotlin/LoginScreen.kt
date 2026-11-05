import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // Logo Animation States
    var startLogoAnim by remember { mutableStateOf(false) }
    // UI Animation States
    var showUi by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Logo Offsets (Sliding in)
    val offsetTopBar by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 0))
    val offsetTopStem by animateIntAsState(if (startLogoAnim) 0 else 2000, tween(600, 100))
    val offsetCenter by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 200))
    val offsetBottomStem by animateIntAsState(if (startLogoAnim) 0 else 2000, tween(600, 300))
    val offsetBottomBar by animateIntAsState(if (startLogoAnim) 0 else -2000, tween(600, 400))

    // UI Fade and Slide Up Animations
    val uiAlpha by animateFloatAsState(if (showUi) 1f else 0f, tween(800))
    val uiOffsetY by animateFloatAsState(if (showUi) 0f else 50f, tween(800))

    LaunchedEffect(Unit) {
        // Start logo sliding in immediately
        startLogoAnim = true
        // Wait for the logo to finish sliding (600ms + 400ms delay = 1000ms)
        delay(1000)
        // Now trigger the UI fade-in
        showUi = true
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            // ANIMATED SMALLER LOGO
            Box(Modifier.offset { IntOffset(offsetTopBar, 0) }.width(120.dp).height(25.dp).background(Color.White))
            Box(Modifier.offset { IntOffset(offsetTopStem, 0) }.width(40.dp).height(35.dp).background(Color.White))
            Box(
                Modifier.offset { IntOffset(offsetCenter, 0) }.width(100.dp).height(50.dp).background(Color.White), 
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(40.dp).height(30.dp).background(Color.Black))
            }
            Box(Modifier.offset { IntOffset(offsetBottomStem, 0) }.width(40.dp).height(35.dp).background(Color.White))
            Box(Modifier.offset { IntOffset(offsetBottomBar, 0) }.width(120.dp).height(25.dp).background(Color.White))

            Spacer(Modifier.height(16.dp))
            
            // "IoN" Text (Can also be animated if you want, but for now it appears with the UI)
            Text(
                "IoN", 
                color = Color.White, 
                fontSize = 36.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(uiAlpha)
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Tagline & Inputs grouped together
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(uiAlpha).offset { IntOffset(0, uiOffsetY.toInt()) }
            ) {
                Text(
                    text = "Simply the best AI automation system you will ever find",
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(32.dp))

                // Google Button
                Button(
                    onClick = { /* TODO: Google Login */ },
                    modifier = Modifier.width(320.dp).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("OR", color = Color(0xFFB3B3B3), fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))

                // Email Field
                Box(
                    modifier = Modifier.width(320.dp).height(45.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        decorationBox = { innerTextField ->
                            if (email.isEmpty()) Text("Email address", color = Color(0xFF777777))
                            innerTextField()
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Password Field
                Box(
                    modifier = Modifier.width(320.dp).height(45.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        decorationBox = { innerTextField ->
                            if (password.isEmpty()) Text("Password", color = Color(0xFF777777))
                            innerTextField()
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Sign In Button
                Button(
                    onClick = { onLoginSuccess() },
                    modifier = Modifier.width(320.dp).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3B3B3)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SIGN IN / REGISTER", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

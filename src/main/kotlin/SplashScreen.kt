import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }
    
    val offsetTopBar by animateIntAsState(if (startAnim) 0 else -2000, tween(600, 0))
    val offsetTopStem by animateIntAsState(if (startAnim) 0 else 2000, tween(600, 100))
    val offsetCenter by animateIntAsState(if (startAnim) 0 else -2000, tween(600, 200))
    val offsetBottomStem by animateIntAsState(if (startAnim) 0 else 2000, tween(600, 300))
    val offsetBottomBar by animateIntAsState(if (startAnim) 0 else -2000, tween(600, 400))

    val textY by animateIntAsState(if (startAnim) 0 else -100, tween(500, 800))
    val textAlpha by animateFloatAsState(if (startAnim) 1f else 0f, tween(500, 800))

    LaunchedEffect(Unit) {
        delay(1000) // Wait for X11 to draw the window
        startAnim = true
        delay(1500) // Wait for animation to finish
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Exact Logo Geometry
            Box(Modifier.offset { IntOffset(offsetTopBar, 0) }.width(240.dp).height(50.dp).background(Color.White))
            Box(Modifier.offset { IntOffset(offsetTopStem, 0) }.width(80.dp).height(70.dp).background(Color.White))
            Box(
                Modifier.offset { IntOffset(offsetCenter, 0) }.width(200.dp).height(100.dp).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(80.dp).height(60.dp).background(Color.Black))
            }
            Box(Modifier.offset { IntOffset(offsetBottomStem, 0) }.width(80.dp).height(70.dp).background(Color.White))
            Box(Modifier.offset { IntOffset(offsetBottomBar, 0) }.width(240.dp).height(50.dp).background(Color.White))

            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "IoN",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset { IntOffset(0, textY) }.alpha(textAlpha)
            )
        }
    }
}

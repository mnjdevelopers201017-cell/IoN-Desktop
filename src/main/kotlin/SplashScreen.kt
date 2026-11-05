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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val screenWidth = 2000f
    val easing = LinearOutSlowInEasing

    val topBarX = remember { Animatable(-screenWidth) }
    val topStemX = remember { Animatable(screenWidth) }
    val centerX = remember { Animatable(-screenWidth) }
    val bottomStemX = remember { Animatable(screenWidth) }
    val bottomBarX = remember { Animatable(-screenWidth) }

    val textY = remember { Animatable(-100f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // CRITICAL: Wait for the window to actually render on the screen before animating
        delay(500) 

        launch { topBarX.animateTo(0f, tween(400, 0, easing = easing)) }
        launch { topStemX.animateTo(0f, tween(400, 100, easing = easing)) }
        launch { centerX.animateTo(0f, tween(400, 200, easing = easing)) }
        launch { bottomStemX.animateTo(0f, tween(400, 300, easing = easing)) }
        launch { bottomBarX.animateTo(0f, tween(400, 400, easing = easing)) }

        delay(650)
        launch { textY.animateTo(0f, tween(350, easing = easing)) }
        launch { textAlpha.animateTo(1f, tween(350)) }

        delay(1500)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Exact Logo Geometry (Flush edges, matching the image)
            Box(Modifier.offset(x = topBarX.value.dp).width(240.dp).height(50.dp).background(Color.White))
            Box(Modifier.offset(x = topStemX.value.dp).width(80.dp).height(70.dp).background(Color.White))
            Box(
                Modifier.offset(x = centerX.value.dp).width(200.dp).height(100.dp).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(80.dp).height(60.dp).background(Color.Black))
            }
            Box(Modifier.offset(x = bottomStemX.value.dp).width(80.dp).height(70.dp).background(Color.White))
            Box(Modifier.offset(x = bottomBarX.value.dp).width(240.dp).height(50.dp).background(Color.White))

            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "IoN",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = textY.value.dp).alpha(textAlpha.value)
            )
        }
    }
}

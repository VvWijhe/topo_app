package nl.topo.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import nl.topo.app.ui.theme.Night
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val base: Float)

/** Diepblauwe nachthemel met zacht twinkelende sterren achter de hele app. */
@Composable
fun NightSky(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val stars = remember {
        val rnd = Random(52)
        List(110) { Star(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.3f, rnd.nextFloat() * 2f * PI.toFloat(), 0.15f + rnd.nextFloat() * 0.45f) }
    }
    val t by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night.skyHigh, Night.sky, Night.skyDeep))),
    ) {
        if (!Night.isLight) Canvas(Modifier.fillMaxSize()) {
            for (s in stars) {
                val a = (s.base + 0.25f * sin(t + s.phase)).coerceIn(0.05f, 0.9f)
                drawCircle(Color.White.copy(alpha = a), radius = s.r, center = Offset(s.x * size.width, s.y * size.height))
            }
        }
        content()
    }
}

package com.vica.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shimmer loading animation modifier.
 * Apply to any composable during loading state to show a sweeping light effect.
 *
 * Usage:
 *   Box(modifier = Modifier.fillMaxWidth().height(200.dp).shimmer())
 */
fun Modifier.shimmer(
    shimmerColor: Color = Color.White.copy(alpha = 0.25f),
    backgroundColor: Color = Color(0xFF1C1C28)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    shimmerColor,
                    Color.Transparent
                ),
                start = Offset(translateAnim - 300f, 0f),
                end   = Offset(translateAnim + 300f, size.height)
            )
        )
    }
}

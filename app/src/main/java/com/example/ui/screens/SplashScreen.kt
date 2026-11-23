package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean?,
    onFinished: (Boolean) -> Unit
) {
    val scale = remember { Animatable(0.84f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val beamProgress by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_y"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500))
    }

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted != null) {
            delay(1300)
            onFinished(onboardingCompleted)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PureWhite, CobaltBlueSoft, PureWhite)
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DeepNavy,
                shadowElevation = 14.dp,
                modifier = Modifier.size(116.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    val w = size.width
                    val h = size.height
                    // Crisp white page sheet
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.18f, h * 0.12f),
                        size = Size(w * 0.64f, h * 0.76f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    // Four Cobalt Blue corner brackets
                    val stroke = Stroke(width = 7f, cap = StrokeCap.Round)
                    val len = w * 0.22f
                    // Top-Left
                    drawLine(CobaltBlue, Offset(0f, len), Offset(0f, 0f), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(CobaltBlue, Offset(0f, 0f), Offset(len, 0f), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Top-Right
                    drawLine(CobaltBlue, Offset(w - len, 0f), Offset(w, 0f), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(CobaltBlue, Offset(w, 0f), Offset(w, len), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Bottom-Left
                    drawLine(CobaltBlue, Offset(0f, h - len), Offset(0f, h), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(CobaltBlue, Offset(0f, h), Offset(len, h), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Bottom-Right
                    drawLine(CobaltBlue, Offset(w - len, h), Offset(w, h), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(CobaltBlue, Offset(w, h - len), Offset(w, h), strokeWidth = 7f, cap = StrokeCap.Round)

                    // Animated optical scan beam
                    val y = h * beamProgress
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF38BDF8), CobaltBlue, Color(0xFF38BDF8))
                        ),
                        start = Offset(w * 0.08f, y),
                        end = Offset(w * 0.92f, y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = DeepNavy,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "100% Local & Private · No Sign-In Required",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp)
                .alpha(alpha.value)
        )
    }
}

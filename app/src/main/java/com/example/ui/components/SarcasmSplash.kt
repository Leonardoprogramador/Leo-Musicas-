package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SarcasmManager
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SarcasmSplash(
    sarcasmLevel: SarcasmManager.SarcasmLevel,
    onSplashFinished: () -> Unit
) {
    val selectedPhrase = remember {
        SarcasmManager.getRandomPhrase(sarcasmLevel) ?: "Iniciando reprodutor..."
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Wait 2 seconds before entering the player
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Beautiful custom Canvas animated glowing soundwave
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val waveCount = 5
                    val spacing = width / (waveCount + 1)

                    // Draw 5 glowing bars pulsing with sin wave
                    for (i in 0 until waveCount) {
                        val barX = spacing * (i + 1)
                        val offsetMultiplier = sin((i * 0.8f) + (pulseScale * 3.14f))
                        val barHeight = (height * 0.4f) + (height * 0.35f * offsetMultiplier)
                        val startY = centerY - barHeight / 2f
                        val endY = centerY + barHeight / 2f

                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.3f))
                            ),
                            start = Offset(barX, startY),
                            end = Offset(barX, endY),
                            strokeWidth = 10.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "LEO MÚSICAS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Displays the sarcastic text
            Text(
                text = "“$selectedPhrase”",
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

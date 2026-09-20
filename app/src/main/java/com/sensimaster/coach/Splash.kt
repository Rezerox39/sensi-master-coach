package com.sensimaster.coach

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val intro by animateFloatAsState(
        targetValue = if (progress > 0f) 1f else 0f,
        animationSpec = tween(450),
        label = "splashIntro"
    )

    LaunchedEffect(Unit) {
        val steps = 30
        repeat(steps) {
            progress = (it + 1) / steps.toFloat()
            delay(45L)
        }
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .graphicsLayer {
                        scaleX = 0.82f + 0.18f * intro
                        scaleY = 0.82f + 0.18f * intro
                        alpha = intro
                    },
                contentAlignment = Alignment.Center
            ) {
                MascotFace("idle", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "SENSI MASTER COACH",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.16f + 0.20f * intro)
                    .align(Alignment.CenterHorizontally)
                    .background(AppAccent)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your Personal FF Sensi Coach",
                color = AppMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AppAccent,
                trackColor = AppSurfaceHigh
            )
            Text(
                "Independent reference app. No game modification.",
                color = AppMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

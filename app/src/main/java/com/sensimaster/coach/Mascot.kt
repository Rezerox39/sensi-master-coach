package com.sensimaster.coach

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Smooth device tilt in -1..1 space. Uses gyroscope when available and
// falls back to accelerometer if the device has no gyro.
@Composable
fun rememberDeviceTilt(): Offset {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = gyro ?: accel
        var sx = 0f
        var sy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val k = 0.16f
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        // rad/s about x tilts eyes vertically, about y horizontally.
                        sx = sx * (1f - k) + (-event.values[1]) * 1.8f * k
                        sy = sy * (1f - k) + event.values[0] * 1.8f * k
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val g = SensorManager.GRAVITY_EARTH
                        sx = sx * (1f - k) + (event.values[1] / g) * 2.2f * k
                        sy = sy * (1f - k) + (event.values[0] / g) * 2.2f * k
                    }
                }
                tilt = Offset(sx.coerceIn(-1f, 1f), sy.coerceIn(-1f, 1f))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return tilt
}

// One smooth blob, no mouth, with eyes that track device tilt and blink.
@Composable
fun MascotFace(expression: String, modifier: Modifier = Modifier) {
    val tilt = rememberDeviceTilt()
    var blink by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2400L + Random.nextLong(1800L))
            blink = true
            delay(130L)
            blink = false
        }
    }

    val glow by animateColorAsState(
        targetValue = when (expression) {
            "success", "happy", "excited" -> SignalGreen
            "concerned", "warning", "confused" -> SignalAmber
            "offline", "sleeping" -> AppMuted
            else -> AppAccent
        },
        label = "glow"
    )

    val eyeOpen by animateFloatAsState(
        targetValue = if (blink) 0.08f else 1f,
        animationSpec = tween(90),
        label = "blink"
    )

    Canvas(modifier = modifier) {
        val side = size.minDimension * 0.90f
        val center = center

        // Soft expression halo.
        drawCircle(
            color = glow.copy(alpha = 0.18f),
            radius = side * 0.60f,
            center = center
        )

        // Simple white circular face, like the Grok bot mark.
        drawCircle(color = Color.White, radius = side / 2f, center = center)
        drawCircle(
            color = glow.copy(alpha = 0.6f),
            radius = side / 2f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Black eyes that track device tilt and blink.
        val eyeW = side * 0.16f
        val eyeH = side * 0.26f * eyeOpen
        val eyeY = center.y
        val shiftX = tilt.x * side * 0.05f
        val shiftY = tilt.y * side * 0.04f
        listOf(center.x - side * 0.16f, center.x + side * 0.16f).forEach { ex ->
            val cx = ex + shiftX
            val cy = eyeY + shiftY
            drawRoundRect(
                color = Color(0xFF101014),
                topLeft = Offset(cx - eyeW / 2f, cy - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeH / 2f)
            )
        }
    }

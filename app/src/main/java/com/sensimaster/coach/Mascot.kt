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
            "offline", "sleeping" -> SensisMuted
            else -> SensisAccent
        },
        label = "glow"
    )

    val eyeOpen by animateFloatAsState(
        targetValue = if (blink) 0.08f else 1f,
        animationSpec = tween(90),
        label = "blink"
    )

    Canvas(modifier = modifier) {
        val side = size.minDimension * 0.86f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f

        // Soft outer halo.
        drawCircle(
            color = glow.copy(alpha = 0.14f),
            radius = side * 0.62f,
            center = center
        )

        // Single smooth blob body.
        val body = Path().apply {
            moveTo(left + side * 0.5f, top)
            cubicTo(left + side * 1.04f, top + side * 0.14f, left + side * 1.04f, top + side * 0.86f, left + side * 0.5f, top + side)
            cubicTo(left - side * 0.04f, top + side * 0.86f, left - side * 0.04f, top + side * 0.14f, left + side * 0.5f, top)
            close()
        }
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                colors = listOf(SensisSurfaceHigh, SensisBg),
                startY = top,
                endY = top + side
            )
        )
        drawPath(
            path = body,
            color = glow.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawPath(
            path = body,
            color = glow.copy(alpha = 0.12f),
            style = Stroke(width = 8.dp.toPx())
        )

        // Eyes.
        val eyeW = side * 0.13f
        val eyeH = side * 0.20f * eyeOpen
        val eyeY = top + side * 0.40f
        val shiftX = tilt.x * side * 0.06f
        val shiftY = tilt.y * side * 0.05f
        val eyeXs = listOf(left + side * 0.34f, left + side * 0.66f)
        eyeXs.forEach { ex ->
            val cx = ex + shiftX
            val cy = eyeY + shiftY
            val socket = Rect(cx - eyeW / 2f, cy - eyeH / 2f, cx + eyeW / 2f, cy + eyeH / 2f)

            // Almond-ish rounded eye with soft glow.
            drawOval(
                color = glow.copy(alpha = 0.25f),
                topLeft = Offset(socket.left - eyeW * 0.22f, socket.top - eyeH * 0.22f),
                size = Size(socket.width + eyeW * 0.44f, socket.height + eyeH * 0.44f)
            )
            drawOval(color = Color(0xFF101216).copy(alpha = 0.92f), topLeft = socket.topLeft, size = socket.size)
            drawOval(color = glow.copy(alpha = 0.5f), topLeft = socket.topLeft, size = socket.size, style = Stroke(1.4.dp.toPx()))

            // Iris + pupil reacting to tilt.
            val pupilR = eyeW * 0.34f
            val irisR = eyeW * 0.52f
            val irisCenter = Offset(cx, cy)
            drawCircle(color = glow, radius = irisR, center = irisCenter)
            drawCircle(color = Color.Black, radius = pupilR, center = irisCenter)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = pupilR * 0.35f,
                center = Offset(irisCenter.x - pupilR * 0.3f, irisCenter.y - pupilR * 0.3f)
            )
        }
    }
}

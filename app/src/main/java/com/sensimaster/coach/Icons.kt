package com.sensimaster.coach

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

enum class TabIcon { Home, Bolt, Gauge, Crosshair, Profile }

// Original minimal line icons drawn on the design system.
@Composable
fun AppIcon(icon: TabIcon, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val stroke = s * 0.08f
        val c = center
        fun pt(x: Float, y: Float) = Offset(c.x + (x - 0.5f) * s, c.y + (y - 0.5f) * s)

        fun drawPath(points: List<Offset>, close: Boolean = false) {
            val path = Path().apply {
                points.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
                if (close) close()
            }
            drawPath(path, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }

        when (icon) {
            TabIcon.Home -> {
                drawPath(listOf(pt(0.5f, 0.16f), pt(0.12f, 0.47f), pt(0.88f, 0.47f)))
                drawPath(
                    listOf(pt(0.24f, 0.47f), pt(0.24f, 0.86f), pt(0.76f, 0.86f), pt(0.76f, 0.47f)),
                    close = true
                )
            }
            TabIcon.Bolt -> {
                drawPath(listOf(pt(0.64f, 0.12f), pt(0.28f, 0.56f), pt(0.5f, 0.56f), pt(0.38f, 0.88f), pt(0.74f, 0.42f), pt(0.52f, 0.42f)))
            }
            TabIcon.Gauge -> {
                val radius = s * 0.30f
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(c.x - radius, c.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val needleEnd = Offset(c.x + radius * 0.72f, c.y - radius * 0.55f)
                drawLine(tint, c, needleEnd, strokeWidth = stroke, cap = StrokeCap.Round)
            }
            TabIcon.Crosshair -> {
                val radius = s * 0.26f
                drawCircle(tint, radius = radius, style = Stroke(stroke))
                drawLine(tint, pt(0.5f, 0.08f), pt(0.5f, 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tint, pt(0.5f, 0.76f), pt(0.5f, 0.92f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tint, pt(0.08f, 0.5f), pt(0.24f, 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tint, pt(0.76f, 0.5f), pt(0.92f, 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            TabIcon.Profile -> {
                drawCircle(tint, radius = s * 0.16f, center = pt(0.5f, 0.33f), style = Stroke(stroke))
                drawArc(
                    color = tint,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = pt(0.14f, 0.56f),
                    size = androidx.compose.ui.geometry.Size(s * 0.72f, s * 0.34f),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

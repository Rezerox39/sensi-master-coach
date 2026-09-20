package com.sensimaster.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val SQRT_HALF = 0.7071f

@Composable
fun CrosshairPreview(config: CrosshairConfig, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(SensisBg, RoundedCornerShape(14.dp))
            .border(1.dp, SensisBorder, RoundedCornerShape(14.dp))
    ) {
        val c = center
        val dim = size.minDimension

        // Faint alignment guides.
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(c.x, 0f),
            end = Offset(c.x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, c.y),
            end = Offset(size.width, c.y),
            strokeWidth = 1.dp.toPx()
        )

        val color = Color(config.color.argb.toInt()).copy(alpha = config.opacity)
        val outline = if (config.outline) Color.Black.copy(alpha = config.opacity) else null
        val thicknessPx = config.thickness.dp.toPx()
        val gapPx = config.gap.dp.toPx()
        val sizePx = (config.size.dp.toPx() * (dim / 220f)).coerceAtMost(dim * 0.42f)
        val dotR = config.dotRadius.dp.toPx() * (dim / 220f)

        fun arm(from: Offset, to: Offset, width: Float, fill: Color) {
            if (outline != null) {
                drawLine(outline, from, to, strokeWidth = width + 3.dp.toPx(), cap = StrokeCap.Round)
            }
            drawLine(fill, from, to, strokeWidth = width, cap = StrokeCap.Round)
        }

        when (config.shape) {
            CrosshairShape.Cross -> {
                arm(Offset(c.x - sizePx, c.y), Offset(c.x - gapPx, c.y), thicknessPx, color)
                arm(Offset(c.x + gapPx, c.y), Offset(c.x + sizePx, c.y), thicknessPx, color)
                arm(Offset(c.x, c.y - sizePx), Offset(c.x, c.y - gapPx), thicknessPx, color)
                arm(Offset(c.x, c.y + gapPx), Offset(c.x, c.y + sizePx), thicknessPx, color)
                if (config.centerDot) dot(c, dotR, color, outline)
            }
            CrosshairShape.Dot -> dot(c, dotR, color, outline)
            CrosshairShape.Circle -> {
                if (outline != null) {
                    drawCircle(outline, radius = sizePx * 0.62f, center = c, style = Stroke(thicknessPx + 3.dp.toPx()))
                }
                drawCircle(color, radius = sizePx * 0.62f, center = c, style = Stroke(thicknessPx))
                if (config.centerDot) dot(c, dotR, color, outline)
            }
            CrosshairShape.Split -> {
                val inner = gapPx
                val outer = sizePx
                listOf(1f to 1f, 1f to -1f, -1f to 1f, -1f to -1f).forEach { (dx, dy) ->
                    val sx = c.x + dx * inner * SQRT_HALF
                    val sy = c.y + dy * inner * SQRT_HALF
                    val ex = c.x + dx * outer * SQRT_HALF
                    val ey = c.y + dy * outer * SQRT_HALF
                    arm(Offset(sx, sy), Offset(ex, ey), thicknessPx, color)
                }
                if (config.centerDot) dot(c, dotR, color, outline)
            }
            CrosshairShape.TShape -> {
                arm(Offset(c.x - sizePx, c.y), Offset(c.x - gapPx, c.y), thicknessPx, color)
                arm(Offset(c.x + gapPx, c.y), Offset(c.x + sizePx, c.y), thicknessPx, color)
                arm(Offset(c.x, c.y + gapPx), Offset(c.x, c.y + sizePx), thicknessPx, color)
                if (config.centerDot) dot(c, dotR, color, outline)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.dot(center: Offset, radius: Float, fill: Color, outline: Color?) {
    if (outline != null) {
        drawCircle(outline, radius = radius + 1.5.dp.toPx(), center = center)
    }
    drawCircle(fill, radius = radius, center = center)
}

@Composable
fun CrosshairLab(
    config: CrosshairConfig,
    onChange: (CrosshairConfig) -> Unit,
    saved: List<CrosshairConfig>,
    onSave: () -> Unit,
    onApplySaved: (CrosshairConfig) -> Unit,
    onDeleteSaved: (Int) -> Unit,
) {
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("Preview")
        CrosshairPreview(config)

        SectionLabel("Presets")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrosshairPresets.series.forEachIndexed { index, preset ->
                Chip(
                    label = crosshairName(index),
                    active = config == preset,
                    onClick = { onChange(preset) }
                )
            }
        }

        SectionLabel("Shape")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrosshairShape.entries.forEach { shape ->
                Chip(label = shape.label, active = config.shape == shape) { onChange(config.copy(shape = shape)) }
            }
        }

        SectionLabel("Color")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CrosshairColor.entries.forEach { color ->
                val active = config.color == color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(if (active) 3.dp else 1.dp, if (active) Color.White else SensisBorder, CircleShape)
                        .background(Color(color.argb.toInt()), CircleShape)
                        .clickable { onChange(config.copy(color = color)) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TogglePill("Center dot", config.centerDot, Modifier.weight(1f)) { onChange(config.copy(centerDot = it)) }
            TogglePill("Outline", config.outline, Modifier.weight(1f)) { onChange(config.copy(outline = it)) }
        }

        SliderRow("Size", "${config.size}px", config.size.toFloat(), 16f..120f, 103) {
            onChange(config.copy(size = it.roundToInt()))
        }
        SliderRow("Thickness", "${config.thickness}px", config.thickness.toFloat(), 1f..12f, 10) {
            onChange(config.copy(thickness = it.roundToInt()))
        }
        SliderRow("Gap", "${config.gap}px", config.gap.toFloat(), 0f..40f, 39) {
            onChange(config.copy(gap = it.roundToInt()))
        }
        SliderRow("Opacity", "${(config.opacity * 100).roundToInt()}%", config.opacity, 0.2f..1f, 0) {
            onChange(config.copy(opacity = it))
        }
        if (config.centerDot) {
            SliderRow("Dot size", "${config.dotRadius}px", config.dotRadius.toFloat(), 2f..14f, 11) {
                onChange(config.copy(dotRadius = it.roundToInt()))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Save preset", onClick = onSave, modifier = Modifier.weight(1f))
            GhostButton(
                if (copied) "Copied" else "Export",
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(crosshairDescription(config)))
                    copied = true
                },
                modifier = Modifier.weight(1f)
            )
        }
        InfoCard(
            "In-app only",
            "This editor previews and exports a crosshair you apply manually. MVP does not draw over the game or anything else."
        )

        SectionLabel("Saved crosshairs")
        if (saved.isEmpty()) {
            InfoCard("Nothing saved yet", "Save a preset to keep it next to your profiles.")
        } else {
            saved.forEachIndexed { index, savedConfig ->
                CardShell {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(crosshairName(index), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "${savedConfig.shape.label} / ${savedConfig.color.label} / ${savedConfig.size}px",
                                color = SensisMuted
                            )
                        }
                        GhostButton("Use") { onApplySaved(savedConfig) }
                        Spacer(Modifier.width(8.dp))
                        GhostButton("Del") { onDeleteSaved(index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (active) SensisAccent else SensisBorder, RoundedCornerShape(20.dp))
            .background(if (active) SensisRedTint else SensisSurface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (active) Color.White else SensisMuted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TogglePill(label: String, checked: Boolean, modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = modifier
            .border(1.dp, if (checked) SensisAccent else SensisBorder, RoundedCornerShape(14.dp))
            .background(if (checked) SensisRedTint else SensisSurface, RoundedCornerShape(14.dp))
            .clickable { onToggle(!checked) }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$label: ${if (checked) "ON" else "OFF"}",
            color = if (checked) Color.White else SensisMuted,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    CardShell {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            Text(valueLabel, color = SensisAccent, fontWeight = FontWeight.Black)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

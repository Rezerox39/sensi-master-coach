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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val SQRT_HALF = 0.7071f

@Composable
fun CrosshairPreview(config: CrosshairConfig, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(SensisBg, RoundedCornerShape(16.dp))
            .border(1.dp, SensisBorder, RoundedCornerShape(16.dp))
    ) {
        val c = center
        val dim = size.minDimension
        val cell = dim / 5f

        // Stage grid.
        for (i in 1..4) {
            val alpha = 0.045f
            drawLine(Color.White.copy(alpha = alpha), Offset(c.x - cell * i, c.y), Offset(c.x + cell * i, c.y), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = alpha), Offset(c.x, c.y - cell * i), Offset(c.x, c.y + cell * i), strokeWidth = 1.dp.toPx())
        }
        drawCircle(Color.White.copy(alpha = 0.07f), radius = 2.2f * cell, center = c, style = Stroke(1.dp.toPx()))

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
                    arm(Offset(c.x + dx * inner * SQRT_HALF, c.y + dy * inner * SQRT_HALF), Offset(c.x + dx * outer * SQRT_HALF, c.y + dy * outer * SQRT_HALF), thicknessPx, color)
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

private fun DrawScope.dot(center: Offset, radius: Float, fill: Color, outline: Color?) {
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Live stage")
        CrosshairPreview(config)

        SectionHeader("Presets")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrosshairPresets.series.forEachIndexed { index, preset ->
                TagChip(
                    crosshairName(index),
                    active = config == preset,
                    onClick = { onChange(preset) }
                )
            }
        }

        SectionHeader("Shape")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrosshairShape.entries.forEach { shape ->
                TagChip(shape.label, active = config.shape == shape, onClick = { onChange(config.copy(shape = shape)) })
            }
        }

        SectionHeader("Color")
        CardShell {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CrosshairColor.entries.forEach { color ->
                    val active = config.color == color
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(if (active) 3.dp else 1.dp, if (active) Color.White else SensisBorder, CircleShape)
                            .background(Color(color.argb.toInt()), CircleShape)
                            .clickable { onChange(config.copy(color = color)) }
                    )
                }
            }
        }

        SectionHeader("Toggles")
        CardShell {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TagChip("Center dot: ${if (config.centerDot) "ON" else "OFF"}", active = config.centerDot, onClick = { onChange(config.copy(centerDot = !config.centerDot)) }, modifier = Modifier.weight(1f))
                TagChip("Outline: ${if (config.outline) "ON" else "OFF"}", active = config.outline, onClick = { onChange(config.copy(outline = !config.outline)) }, modifier = Modifier.weight(1f))
            }
        }

        SectionHeader("Dimensions")
        SliderRow("Size", "${config.size}px", config.size.toFloat(), 16f..120f, 103) { onChange(config.copy(size = it.roundToInt())) }
        SliderRow("Thickness", "${config.thickness}px", config.thickness.toFloat(), 1f..12f, 10) { onChange(config.copy(thickness = it.roundToInt())) }
        SliderRow("Gap", "${config.gap}px", config.gap.toFloat(), 0f..40f, 39) { onChange(config.copy(gap = it.roundToInt())) }
        SliderRow("Opacity", "${(config.opacity * 100).roundToInt()}%", config.opacity, 0.2f..1f, 0) { onChange(config.copy(opacity = it)) }
        if (config.centerDot) {
            SliderRow("Dot size", "${config.dotRadius}px", config.dotRadius.toFloat(), 2f..14f, 11) { onChange(config.copy(dotRadius = it.roundToInt())) }
        }

        SectionHeader("Actions")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Save preset", modifier = Modifier.weight(1f)) { onSave() }
            GhostButton(if (copied) "Copied" else "Export", modifier = Modifier.weight(1f)) {
                clipboard.setText(AnnotatedString(crosshairDescription(config)))
                copied = true
            }
        }
        InfoCard(
            "In-app only",
            "This editor previews and exports a crosshair you apply manually. MVP does not draw over the game or anything else."
        )

        SectionHeader("Saved crosshairs")
        if (saved.isEmpty()) {
            CardShell {
                Text("Nothing saved yet. Save a preset to keep it next to your profiles.", color = SensisMuted)
            }
        } else {
            saved.forEachIndexed { index, savedConfig ->
                CardShell {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(crosshairName(index), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${savedConfig.shape.label} / ${savedConfig.color.label} / ${savedConfig.size}px", color = SensisMuted, style = MaterialTheme.typography.labelMedium)
                        }
                        GhostButton("Use") { onApplySaved(savedConfig) }
                        GhostButton("Del") { onDeleteSaved(index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    CardShell {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            Text(valueLabel, color = SensisAccent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(4.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

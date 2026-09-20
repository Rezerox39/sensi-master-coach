package com.sensimaster.coach

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(AppAccent, RoundedCornerShape(2.dp))
        )
        Text(title.uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = AppMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TagChip(
    text: String,
    active: Boolean = false,
    tint: Color = AppAccent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .border(1.dp, if (active) tint else AppBorder, RoundedCornerShape(20.dp))
            .background(if (active) tint.copy(alpha = 0.16f) else AppSurface, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (active) tint else AppMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ValueChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .background(AppSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurfaceLow, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            val bg by animateColorAsState(
                targetValue = if (active) AppPrimary else Color.Transparent,
                label = "segment"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labelOf(option),
                    color = if (active) Color.White else AppMuted,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun GaugeBar(value: Int, max: Int = 100, color: Color = AppAccent, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "gauge"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(AppSurfaceHigh, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "press"
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(AppSurface, RoundedCornerShape(18.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier, accent: Color = AppAccent) {
    Column(
        modifier = modifier
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .background(AppSurface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}

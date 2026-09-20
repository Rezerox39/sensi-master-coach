package com.sensimaster.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Arise-inspired palette: deep near-black surfaces, navy-blue primary, electric-blue accents,
// orange secondary highlights, and muted gray text on white.
val AppBg = Color(0xFF0D0F12)
val AppSurface = Color(0xFF181A1E)
val AppSurfaceHigh = Color(0xFF20242B)
val AppSurfaceLow = Color(0xFF14161A)
val AppBorder = Color(0xFF2A2F37)
val AppAccent = Color(0xFF3E9BFF)
val AppPrimary = Color(0xFF005488)
val AppPrimarySoft = Color(0x1F3E9BFF)
val AppOrange = Color(0xFFFF9500)
val AppMuted = Color(0xFF8E98A3)
val SignalGreen = Color(0xFF69F0AE)
val SignalAmber = Color(0xFFFFC857)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppBg,
            surface = AppSurface,
            surfaceVariant = AppSurfaceLow,
            primary = AppAccent,
            secondary = AppOrange,
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.Black
        ),
        content = content
    )
}

@Composable
fun CardShell(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(18.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun InfoCard(title: String, body: String) {
    CardShell {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = AppMuted)
    }
}

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary, contentColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = AppSurfaceHigh, contentColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(label)
    }
}

@Composable
fun <T> ChoiceRow(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit, textOf: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
        values.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val active = item == selected
                    val text = textOf(item)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (active) AppAccent else AppBorder, RoundedCornerShape(12.dp))
                            .background(if (active) AppPrimarySoft else AppSurface, RoundedCornerShape(12.dp))
                            .clickable { onSelect(item) }
                            .padding(12.dp)
                    ) {
                        Text(text, color = if (active) Color.White else AppMuted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SectionLabel(label: String) {
    Text(
        label.uppercase(),
        color = AppAccent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black
    )
}

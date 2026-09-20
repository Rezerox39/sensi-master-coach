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

// Sensis-style palette: matte black surfaces, gray panels, red accents, gold highlights.
val SensisBg = Color(0xFF0D0D0D)
val SensisSurface = Color(0xFF151515)
val SensisSurfaceHigh = Color(0xFF1B1B1B)
val SensisSurfaceLow = Color(0xFF141414)
val SensisBorder = Color(0xFF2E2E2E)
val SensisAccent = Color(0xFFFF1F1F)
val SensisAccentDeep = Color(0xFFC60000)
val SensisRedTint = Color(0xFF3E0E0E)
val SensisGold = Color(0xFFFFD700)
val SensisMuted = Color(0xFF9AA0AA)
val SignalGreen = Color(0xFF69F0AE)
val SignalAmber = Color(0xFFFFC857)

@Composable
fun SensisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SensisBg,
            surface = SensisSurface,
            surfaceVariant = SensisSurfaceLow,
            primary = SensisAccent,
            secondary = SensisGold,
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
            .background(SensisSurface, RoundedCornerShape(18.dp))
            .border(1.dp, SensisBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun InfoCard(title: String, body: String) {
    CardShell {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = SensisMuted)
    }
}

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = SensisAccentDeep, contentColor = Color.White),
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
        colors = ButtonDefaults.buttonColors(containerColor = SensisSurfaceHigh, contentColor = Color.White),
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
                            .border(1.dp, if (active) SensisAccent else SensisBorder, RoundedCornerShape(12.dp))
                            .background(if (active) SensisRedTint else SensisSurface, RoundedCornerShape(12.dp))
                            .clickable { onSelect(item) }
                            .padding(12.dp)
                    ) {
                        Text(text, color = if (active) Color.White else SensisMuted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
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
        color = SensisAccent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black
    )
}

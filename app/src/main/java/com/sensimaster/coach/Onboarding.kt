package com.sensimaster.coach

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class SetupResult(
    val playStyle: PlayStyle,
    val experience: Experience,
    val controlStyle: ControlStyle,
    val dpi: Int,
    val refreshRate: Int,
)

@Composable
fun SetupWizard(device: DeviceSnapshot, detectedRefresh: Int, onDone: (SetupResult) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var playStyle by remember { mutableStateOf(PlayStyle.Balanced) }
    var experience by remember { mutableStateOf(Experience.Intermediate) }
    var controlStyle by remember { mutableStateOf(ControlStyle.ThreeFinger) }
    var dpi by remember { mutableIntStateOf(420) }
    val lastStep = 5

    Box(modifier = Modifier.fillMaxSize().background(SensisBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (step == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MascotFace("thinking", modifier = Modifier.size(200.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Build your sensi setup",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "A few quick choices - you tune everything later in calibration.",
                        color = SensisMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Start Setup", modifier = Modifier.fillMaxWidth()) { step = 1 }
                }
            } else {
                WizardProgress(step, lastStep)
                when (step) {
                    1 -> DeviceAndDpiStep(device, detectedRefresh, dpi, { dpi = it })
                    2 -> ChoiceRow("Play style", PlayStyle.entries, playStyle, { playStyle = it }, PlayStyle::label)
                    3 -> ChoiceRow("Experience", Experience.entries, experience, { experience = it }, Experience::label)
                    4 -> ChoiceRow("Control style", ControlStyle.entries, controlStyle, { controlStyle = it }, ControlStyle::label)
                    5 -> SummaryStep(playStyle, experience, controlStyle, dpi, detectedRefresh)
                }
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 1) {
                        GhostButton("Back", modifier = Modifier.weight(1f)) { step-- }
                    }
                    if (step < lastStep) {
                        PrimaryButton("Next", modifier = Modifier.weight(1f)) { step++ }
                    } else {
                        PrimaryButton(
                            "Create my setup",
                            modifier = Modifier.weight(1f)
                        ) {
                            onDone(SetupResult(playStyle, experience, controlStyle, dpi, detectedRefresh))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardProgress(step: Int, lastStep: Int) {
    val fraction by animateFloatAsState(
        targetValue = step / (lastStep + 1f),
        animationSpec = tween(350),
        label = "wizardProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Setup", trailing = "Step $step / $lastStep")
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = SensisAccent,
            trackColor = SensisSurfaceHigh
        )
    }
}

@Composable
private fun DeviceAndDpiStep(
    device: DeviceSnapshot,
    detectedRefresh: Int,
    dpi: Int,
    onDpiChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Device")
        CardShell {
            Text("${device.manufacturer} ${device.model}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Android ${device.androidVersion}  |  ~$detectedRefresh Hz display", color = SensisMuted)
        }
        SectionHeader("Screen DPI")
        CardShell {
            Text("The display density the game uses. Know your value before applying presets.", color = SensisMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DPI", color = Color.White, fontWeight = FontWeight.Bold)
                Text("$dpi", color = SensisAccent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            Slider(
                value = dpi.toFloat(),
                onValueChange = { onDpiChange(it.roundToInt()) },
                valueRange = 320f..640f,
                steps = 63
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("320", color = SensisMuted, style = MaterialTheme.typography.labelSmall)
                Text("480", color = SensisMuted, style = MaterialTheme.typography.labelSmall)
                Text("640", color = SensisMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        InfoCard(
            "Why it matters",
            "Higher DPI shows more view area, so recommended values shift. The generator adjusts automatically."
        )
    }
}

@Composable
private fun SummaryStep(
    playStyle: PlayStyle,
    experience: Experience,
    controlStyle: ControlStyle,
    dpi: Int,
    refreshRate: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Review your setup")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueChip("Style", playStyle.label, Modifier.weight(1f))
            ValueChip("Fingers", controlStyle.label, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueChip("Level", experience.label, Modifier.weight(1f))
            ValueChip("Refresh", "~$refreshRate Hz", Modifier.weight(1f))
        }
        CardShell {
            Text("DPI", color = SensisMuted, style = MaterialTheme.typography.labelMedium)
            Text("$dpi", color = SensisAccent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "The generator turns these choices into testable starting values. Calibration tunes one value at a time.",
                color = SensisMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

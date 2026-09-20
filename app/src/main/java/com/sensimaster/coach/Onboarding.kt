package com.sensimaster.coach

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
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MascotFace("thinking", modifier = Modifier.size(190.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Build your sensi setup",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "A few quick choices. No wrong answers - you tune everything later in calibration.",
                        color = SensisMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Start Setup", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Step ${step} / $lastStep")
                    Text("${(step * 100 / (lastStep + 1))}%", color = SensisMuted, style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(
                    progress = { step / (lastStep + 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = SensisAccent,
                    trackColor = SensisSurfaceHigh
                )
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
                        GhostButton("Back", onClick = { step-- }, modifier = Modifier.weight(1f))
                    }
                    if (step < lastStep) {
                        PrimaryButton("Next", onClick = { step++ }, modifier = Modifier.weight(1f))
                    } else {
                        PrimaryButton(
                            "Create my setup",
                            onClick = {
                                onDone(SetupResult(playStyle, experience, controlStyle, dpi, detectedRefresh))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
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
        SectionLabel("Device")
        CardShell {
            Text("${device.manufacturer} ${device.model}", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Android ${device.androidVersion}  |  ~${detectedRefresh}Hz display", color = SensisMuted)
        }
        SectionLabel("Screen DPI")
        CardShell {
            Text("Display density used by the game. Know your value before applying presets.", color = SensisMuted)
            Spacer(Modifier.height(14.dp))
            Text("$dpi DPI", color = SensisAccent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Your setup")
        CardShell {
            SummaryRow("Play style", playStyle.label)
            SummaryRow("Experience", experience.label)
            SummaryRow("Control style", controlStyle.label)
            SummaryRow("DPI", "$dpi")
            SummaryRow("Refresh rate", "~$refreshRate Hz")
        }
        InfoCard(
            "Starting point, not magic numbers",
            "The generator turns these choices into testable values. Calibration tunes one value at a time."
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SensisMuted)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

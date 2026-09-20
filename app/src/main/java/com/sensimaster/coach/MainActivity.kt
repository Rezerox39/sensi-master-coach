package com.sensimaster.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Tab(val label: String) {
    Home("Home"),
    Sensi("Sensi"),
    Calibrate("Calibrate"),
    Crosshair("Crosshair"),
    Profiles("Profiles"),
}

data class UiState(
    val showSplash: Boolean = true,
    val onboardingComplete: Boolean = false,
    val tab: Tab = Tab.Home,
    val profile: PlayerProfile,
    val recommendations: List<ScopeRecommendation>,
    val history: List<String> = emptyList(),
    val coach: CoachState = CoachState("idle", "Ready to build a setup you can test manually."),
    val crosshair: CrosshairConfig = CrosshairPresets.cross,
    val savedCrosshairs: List<CrosshairConfig> = listOf(
        CrosshairPresets.dot,
        CrosshairPresets.circle,
        CrosshairPresets.split,
    ),
)

class SensiViewModel : ViewModel() {
    private val device = DeviceSnapshot(
        manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.titlecase() },
        model = Build.MODEL.orEmpty().ifBlank { "Android device" },
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        refreshRate = 60,
        width = 0,
        height = 0,
    )
    private val initialProfile = defaultProfile(device)

    var state by mutableStateOf(
        UiState(
            profile = initialProfile,
            recommendations = RecommendationEngine.generate(initialProfile)
        )
    )
        private set

    fun dismissSplash() {
        state = state.copy(showSplash = false)
    }

    fun completeSetup(result: SetupResult) {
        val profile = state.profile.copy(
            playStyle = result.playStyle,
            experience = result.experience,
            controlStyle = result.controlStyle,
            device = state.profile.device.copy(dpi = result.dpi, refreshRate = result.refreshRate)
        )
        state = state.copy(
            onboardingComplete = true,
            tab = Tab.Home,
            profile = profile,
            recommendations = RecommendationEngine.generate(profile),
            coach = CoachState("success", "First setup created. Test it, then adjust one value at a time.")
        )
    }

    fun select(tab: Tab) {
        state = state.copy(
            tab = tab,
            coach = when (tab) {
                Tab.Home -> CoachState("idle", "Pick a loop: generate, test, adjust, save.")
                Tab.Sensi -> CoachState("thinking", "These are starting points, not magic numbers.")
                Tab.Calibrate -> CoachState("focused", "Run one test, change one value, then retest.")
                Tab.Crosshair -> CoachState("analyzing", "Design a crosshair you can read at a glance.")
                Tab.Profiles -> CoachState("happy", "Profiles help you separate rush, sniper, and experimental setups.")
            }
        )
    }

    fun regenerate() {
        state = state.copy(
            recommendations = RecommendationEngine.generate(state.profile),
            coach = CoachState("excited", "Fresh recommendations are ready. Test before saving.")
        )
    }

    fun applyRecommendation(item: ScopeRecommendation) {
        val nextProfile = state.profile.copy(current = state.profile.current.withValue(item.scope, item.value))
        state = state.copy(
            profile = nextProfile,
            recommendations = RecommendationEngine.generate(nextProfile),
            history = state.history + "Applied ${item.scope.label}: ${item.value}",
            coach = CoachState("success", "${item.scope.label} set to ${item.value}. Retest this value before changing another.")
        )
    }

    fun calibrate(scope: ScopeType, signal: CalibrationSignal) {
        val (next, message) = CalibrationEngine.adjust(state.profile.current, scope, signal)
        val nextProfile = state.profile.copy(current = next)
        state = state.copy(
            profile = nextProfile,
            recommendations = RecommendationEngine.generate(nextProfile, listOf(signal)),
            history = state.history + message,
            coach = CoachState(if (signal == CalibrationSignal.Good) "happy" else "concerned", message)
        )
    }

    fun updateCrosshair(config: CrosshairConfig) {
        state = state.copy(crosshair = config)
    }

    fun saveCrosshair() {
        state = state.copy(
            savedCrosshairs = state.savedCrosshairs + state.crosshair,
            coach = CoachState("success", "Crosshair saved to your profiles area.")
        )
    }

    fun applySavedCrosshair(config: CrosshairConfig) {
        state = state.copy(
            crosshair = config,
            coach = CoachState("happy", "Crosshair loaded. Tweak it further or export it.")
        )
    }

    fun deleteSavedCrosshair(index: Int) {
        state = state.copy(
            savedCrosshairs = state.savedCrosshairs.filterIndexed { i, _ -> i != index },
            coach = CoachState("idle", "Crosshair removed.")
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SensiMasterApp() }
    }
}

@Composable
fun SensiMasterApp(vm: SensiViewModel = viewModel()) {
    val context = LocalContext.current
    val detectedRefresh = remember { context.display?.refreshRate?.toInt() ?: 60 }
    SensisTheme {
        Surface(color = SensisBg, modifier = Modifier.fillMaxSize()) {
            when {
                vm.state.showSplash -> SplashScreen(vm::dismissSplash)
                !vm.state.onboardingComplete -> SetupWizard(vm.state.profile.device, detectedRefresh, vm::completeSetup)
                else -> MainShell(vm)
            }
        }
    }
}

@Composable
private fun MainShell(vm: SensiViewModel) {
    val state = vm.state
    Scaffold(
        containerColor = SensisBg,
        bottomBar = {
            NavigationBar(containerColor = SensisSurfaceLow) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { vm.select(tab) },
                        icon = { Text(tab.label.first().toString()) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SensisBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header("Sensi Master Coach", "Independent Free Fire companion")
                CoachPanel(state.coach)
            }
            when (state.tab) {
                Tab.Home -> item { HomeScreen(state) }
                Tab.Sensi -> item { SensiScreen(state, vm::regenerate, vm::applyRecommendation) }
                Tab.Calibrate -> item { CalibrationScreen(state, vm::calibrate) }
                Tab.Crosshair -> item {
                    CrosshairLab(
                        config = state.crosshair,
                        onChange = vm::updateCrosshair,
                        saved = state.savedCrosshairs,
                        onSave = vm::saveCrosshair,
                        onApplySaved = vm::applySavedCrosshair,
                        onDeleteSaved = vm::deleteSavedCrosshair
                    )
                }
                Tab.Profiles -> item { ProfilesScreen(state) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, color = SensisMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CoachPanel(coach: CoachState) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MascotFace(coach.expression, modifier = Modifier.width(96.dp).height(96.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coach", color = SensisAccent, fontWeight = FontWeight.Bold)
                Text(coach.message, color = Color.White)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatGrid(state)
        InfoCard("Core loop", "Device -> Profile -> Generate -> Test -> Analyze -> Adjust -> Retest -> Save -> Improve")
        InfoCard("DPI & refresh", "${state.profile.device.dpi} DPI | ~${state.profile.device.refreshRate} Hz on this profile.")
        InfoCard("Safety", "All values are references you manually apply. This app does not modify Free Fire.")
    }
}

@Composable
private fun SensiScreen(
    state: UiState,
    onRegenerate: () -> Unit,
    onApply: (ScopeRecommendation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton("Generate Personalized Setup", onClick = onRegenerate, modifier = Modifier.fillMaxWidth())
        state.recommendations.forEach { item ->
            CardShell {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.scope.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${item.value}  |  range ${item.range.first}-${item.range.last}", color = SensisAccent)
                        Text("${item.confidence}% confidence - ${item.direction}", color = SensisMuted)
                        Text(item.explanation, color = SensisMuted)
                    }
                    GhostButton("Apply") { onApply(item) }
                }
            }
        }
    }
}

@Composable
private fun CalibrationScreen(state: UiState, onSignal: (ScopeType, CalibrationSignal) -> Unit) {
    var scope by remember { mutableStateOf(ScopeType.RedDot) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceRow("Scope", ScopeType.entries, scope, { scope = it }, ScopeType::label)
        val value = state.profile.current.valueFor(scope)
        InfoCard("Current ${scope.label}", "$value. Run one test and choose the actual result.")
        Slider(value = value.toFloat(), onValueChange = {}, valueRange = 0f..100f, enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Overshoot", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Overshoot) }
            GhostButton("Good", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Good) }
            GhostButton("Undershoot", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Undershoot) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Shaky", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Shaky) }
            GhostButton("Slow", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Slow) }
        }
        HistoryList(state.history)
    }
}

@Composable
private fun ProfilesScreen(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardShell {
            Text(state.profile.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${state.profile.playStyle.label} / ${state.profile.experience.label} / ${state.profile.controlStyle.label}", color = SensisMuted)
            Text("Weapon preference: ${state.profile.preferredWeapon}", color = SensisMuted)
            Text("Device: ${state.profile.device.model} | ${state.profile.device.dpi} DPI | ~${state.profile.device.refreshRate} Hz", color = SensisMuted)
        }
        ExportPreview(state.profile)
        HistoryList(state.history)
    }
}

@Composable
private fun StatGrid(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("General", state.profile.current.general.toString(), Modifier.weight(1f))
            Metric("Red Dot", state.profile.current.redDot.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("4x", state.profile.current.fourX.toString(), Modifier.weight(1f))
            Metric("Sniper", state.profile.current.sniper.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    CardShell(modifier) {
        Text(label, color = SensisMuted)
        Text(value, color = SensisAccent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}

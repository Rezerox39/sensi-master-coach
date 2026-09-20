package com.sensimaster.coach

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Tab(val label: String, val icon: TabIcon) {
    Home("Home", TabIcon.Home),
    Sensi("Sensi", TabIcon.Bolt),
    Calibrate("Calibrate", TabIcon.Gauge),
    Crosshair("Crosshair", TabIcon.Crosshair),
    Profiles("Profile", TabIcon.Profile),
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
            motivations = result.motivations,
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

    fun resetHistory() {
        state = state.copy(
            history = emptyList(),
            coach = CoachState("idle", "Improvement history cleared.")
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF0D0D0D.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF0D0D0D.toInt())
        )
        setContent { SensiMasterApp() }
    }
}

@Composable
fun SensiMasterApp(vm: SensiViewModel = viewModel()) {
    val context = LocalContext.current
    val detectedRefresh = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            context.display?.refreshRate?.toInt() ?: 60
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.refreshRate.toInt().takeIf { it > 0 } ?: 60
        }
    }
    AppTheme {
        Surface(color = AppBg, modifier = Modifier.fillMaxSize()) {
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
        containerColor = AppBg,
        bottomBar = { AppBottomBar(current = state.tab, onSelect = vm::select) }
    ) { padding ->
        Crossfade(targetState = state.tab, label = "tab") { tab ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(AppBg)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp)
            ) {
                item { AppHeader() }
                item {
                    CoachCard(
                        coach = state.coach,
                        motivations = state.profile.motivations
                    ) { vm.select(Tab.Calibrate) }
                }
                when (tab) {
                    Tab.Home -> item { HomeScreen(state) { vm.select(it) } }
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
                    Tab.Profiles -> item { ProfilesScreen(state, { vm.select(it) }, vm::resetHistory) }
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "SENSI MASTER COACH",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text("Independent Free Fire companion", color = AppMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AppBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Surface(color = AppSurfaceLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp)
        ) {
            Tab.entries.forEach { tab ->
                val selected = tab == current
                val color by animateColorAsState(
                    targetValue = if (selected) AppAccent else AppMuted,
                    label = "tabTint"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AppPrimarySoft, RoundedCornerShape(12.dp))
                            )
                        }
                        AppIcon(tab.icon, Modifier.size(22.dp), tint = color)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachCard(
    coach: CoachState,
    motivations: List<Motivation>,
    onCalibrate: () -> Unit,
) {
    CardShell {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MascotFace(coach.expression, modifier = Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coach", color = AppAccent, fontWeight = FontWeight.Black)
                    TagChip("online", active = true, tint = SignalGreen)
                }
                Text(coach.message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                if (motivations.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        motivations.take(2).forEach { motivation ->
                            TagChip(motivation.label, active = true, tint = AppOrange)
                        }
                    }
                }
                PrimaryButton("Start a calibration test", modifier = Modifier.fillMaxWidth(), onClick = onCalibrate)
            }
        }
    }
}

@Composable
private fun HomeScreen(state: UiState, onNavigate: (Tab) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(icon = TabIcon.Bolt, title = "Generate setup", subtitle = "Sensi values", Modifier.weight(1f)) { onNavigate(Tab.Sensi) }
            QuickAction(icon = TabIcon.Gauge, title = "Test & adjust", subtitle = "Calibration", Modifier.weight(1f)) { onNavigate(Tab.Calibrate) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(icon = TabIcon.Crosshair, title = "Crosshair lab", subtitle = "Design & save", Modifier.weight(1f)) { onNavigate(Tab.Crosshair) }
            QuickAction(icon = TabIcon.Profile, title = "Profiles", subtitle = "Export & history", Modifier.weight(1f)) { onNavigate(Tab.Profiles) }
        }

        SectionHeader("Your setup")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TagChip(state.profile.playStyle.label, active = true)
            TagChip("${state.profile.experience.label}", active = true, tint = AppOrange)
            TagChip("${state.profile.controlStyle.label}", active = true, tint = AppMuted)
            TagChip("${state.profile.device.dpi} DPI")
            TagChip("~${state.profile.device.refreshRate} Hz")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("General", state.profile.current.general.toString(), Modifier.weight(1f))
            StatBlock("Red Dot", state.profile.current.redDot.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("4x", state.profile.current.fourX.toString(), Modifier.weight(1f))
            StatBlock("Sniper", state.profile.current.sniper.toString(), Modifier.weight(1f))
        }

        SectionHeader("The loop")
        InfoCard(
            "Method",
            "Device -> Profile -> Generate -> Test -> Analyze -> Adjust -> Retest -> Save -> Improve. One change at a time."
        )
        InfoCard(
            "Safety",
            "All values are references you apply manually. This app never modifies Free Fire."
        )
    }
}

@Composable
private fun QuickAction(icon: TabIcon, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PressableCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AppPrimarySoft, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(icon, Modifier.size(22.dp), tint = AppAccent)
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AppMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SensiScreen(
    state: UiState,
    onRegenerate: () -> Unit,
    onApply: (ScopeRecommendation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Sensi generator")
        PressableCard(onClick = onRegenerate) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(AppPrimarySoft, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(TabIcon.Bolt, Modifier.size(22.dp), tint = AppAccent)
                }
                Column(Modifier.weight(1f)) {
                    Text("Generate personalized setup", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Combines device, DPI, play style, and history", color = AppMuted, style = MaterialTheme.typography.labelMedium)
                }
                Text("⟳", color = AppAccent, fontWeight = FontWeight.Black)
            }
        }

        state.recommendations.forEach { item ->
            CardShell {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.scope.label, color = Color.White, fontWeight = FontWeight.Bold)
                            TagChip(item.direction, active = true, tint = if (item.direction.startsWith("Increase")) SignalGreen else if (item.direction.startsWith("Decrease")) SignalAmber else AppMuted)
                        }
                        Text(
                            "${item.value}  ·  range ${item.range.first}-${item.range.last}",
                            color = AppAccent,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        GaugeBar(item.confidence)
                        Text("${item.confidence}% confidence", color = AppMuted, style = MaterialTheme.typography.labelSmall)
                        Text(item.explanation, color = AppMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(10.dp))
                    GhostButton("Apply") { onApply(item) }
                }
            }
        }
    }
}

@Composable
private fun CalibrationScreen(state: UiState, onSignal: (ScopeType, CalibrationSignal) -> Unit) {
    var scope by remember { mutableStateOf(ScopeType.RedDot) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Calibration")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScopeType.entries.forEach { option ->
                TagChip(
                    option.label,
                    active = option == scope,
                    tint = if (option == scope) AppAccent else AppMuted,
                    onClick = { scope = option }
                )
            }
        }
        CardShell {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(scope.label, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Current value", color = AppMuted, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    state.profile.current.valueFor(scope).toString(),
                    color = AppAccent,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(12.dp))
            GaugeBar(state.profile.current.valueFor(scope), color = AppAccent)
            Spacer(Modifier.height(6.dp))
            Text("Run one test, then record what actually happened.", color = AppMuted, style = MaterialTheme.typography.bodySmall)
        }
        SectionHeader("Result")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Overshoot", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Overshoot) }
            GhostButton("Good", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Good) }
            GhostButton("Under", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Undershoot) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Shaky", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Shaky) }
            GhostButton("Slow", modifier = Modifier.weight(1f)) { onSignal(scope, CalibrationSignal.Slow) }
        }
        HistoryList(state.history)
    }
}

@Composable
private fun ProfilesScreen(state: UiState, onNavigate: (Tab) -> Unit, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Profile")
        CardShell {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppPrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.profile.name.take(1).uppercase(),
                        color = AppAccent,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Column {
                    Text(state.profile.name, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text("${state.profile.playStyle.label} / ${state.profile.controlStyle.label} / ${state.profile.preferredWeapon}", color = AppMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TagChip("${state.profile.device.model}", active = true)
                TagChip("${state.profile.device.dpi} DPI", active = true, tint = AppOrange)
                TagChip("~${state.profile.device.refreshRate} Hz", active = true, tint = AppMuted)
            }
            if (state.profile.motivations.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    state.profile.motivations.forEach { motivation ->
                        TagChip(motivation.label, active = true, tint = AppAccent)
                    }
                }
            }
        }
        SectionHeader("Values")
        CardShell {
            ScopeType.entries.forEach { scope ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(scope.label, color = AppMuted)
                    Text(
                        state.profile.current.valueFor(scope).toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        ExportPreview(state.profile)
        HistoryList(state.history)

        SectionHeader("Settings")
        PressableCard(onClick = { onNavigate(Tab.Calibrate) }) {
            Text("Start calibration test", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Re-measure one value at a time", color = AppMuted, style = MaterialTheme.typography.labelMedium)
        }
        PressableCard(onClick = onReset) {
            Text("Clear improvement history", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Removes calibration log entries", color = AppMuted, style = MaterialTheme.typography.labelMedium)
        }
        InfoCard(
            "About this app",
            "Sensi Master Coach v1.0.0 is an independent Free Fire reference app. It does not modify, inject into, or automate the game."
        )
    }
}

@Composable
private fun ExportPreview(profile: PlayerProfile) {
    val export = """
        {
          "name": "${profile.name}",
          "general": ${profile.current.general},
          "redDot": ${profile.current.redDot},
          "twoX": ${profile.current.twoX},
          "fourX": ${profile.current.fourX},
          "sniper": ${profile.current.sniper},
          "freeLook": ${profile.current.freeLook},
          "dpi": ${profile.device.dpi},
          "refreshRate": ${profile.device.refreshRate}
        }
    """.trimIndent()
    CardShell {
        SectionHeader("Export preview")
        Spacer(Modifier.height(8.dp))
        Text(export, color = AppMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Text("Copy into your own notes or sync later.", color = AppMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HistoryList(history: List<String>) {
    SectionHeader("Improvement history")
    CardShell {
        if (history.isEmpty()) {
            Text("No calibration changes yet.", color = AppMuted)
        } else {
            history.takeLast(6).forEachIndexed { index, message ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(top = 6.dp)
                            .background(AppAccent, CircleShape)
                    )
                    Column {
                        Text(message, color = AppMuted, style = MaterialTheme.typography.bodySmall)
                        if (index < history.takeLast(6).lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}


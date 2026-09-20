package com.sensimaster.coach

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

private val Amoled = Color(0xFF000000)
private val Ink = Color(0xFF07090D)
private val Panel = Color(0xFF10131A)
private val PanelSoft = Color(0xFF161A22)
private val Cyan = Color(0xFF00E5FF)
private val Lime = Color(0xFF8BFF6A)
private val Amber = Color(0xFFFFC857)
private val Danger = Color(0xFFFF5A7A)
private val TextMuted = Color(0xFFAEB7C6)

enum class Tab(val label: String) {
    Home("Home"),
    Sensi("Sensi"),
    Calibrate("Calibrate"),
    Tools("Tools"),
    Profiles("Profiles"),
}

data class UiState(
    val onboardingComplete: Boolean = false,
    val tab: Tab = Tab.Home,
    val profile: PlayerProfile,
    val recommendations: List<ScopeRecommendation>,
    val history: List<String> = emptyList(),
    val coach: CoachState = CoachState("idle", "Ready to build a setup you can test manually."),
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

    fun completeOnboarding(playStyle: PlayStyle, experience: Experience, controlStyle: ControlStyle) {
        val profile = state.profile.copy(
            playStyle = playStyle,
            experience = experience,
            controlStyle = controlStyle
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
                Tab.Tools -> CoachState("analyzing", "Tools explain setup choices without touching the game.")
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
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SensiMasterApp() }
    }
}

@Composable
fun SensiMasterApp(vm: SensiViewModel = viewModel()) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Amoled,
            surface = Panel,
            primary = Cyan,
            secondary = Lime,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(color = Amoled, modifier = Modifier.fillMaxSize()) {
            if (!vm.state.onboardingComplete) {
                Onboarding(vm::completeOnboarding)
            } else {
                MainShell(vm)
            }
        }
    }
}

@Composable
private fun Onboarding(onDone: (PlayStyle, Experience, ControlStyle) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var playStyle by remember { mutableStateOf(PlayStyle.Balanced) }
    var experience by remember { mutableStateOf(Experience.Intermediate) }
    var controlStyle by remember { mutableStateOf(ControlStyle.ThreeFinger) }
    val screens = listOf("Welcome", "Device", "Play Style", "Experience", "Control", "Current Sensi", "First Setup")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Amoled)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header("Sensi Master Coach", "Your Personal FF Sensi Coach")
            Mascot("thinking", modifier = Modifier.fillMaxWidth().height(150.dp))
            Text("Step ${step + 1} of ${screens.size}: ${screens[step]}", color = Cyan, fontWeight = FontWeight.Bold)
        }
        item {
            when (step) {
                0 -> InfoCard("Independent coach", "Manual sensitivity reference only. No game modification, automation, macros, or guaranteed results.")
                1 -> DeviceCard()
                2 -> ChoiceRow("Play style", PlayStyle.entries, playStyle) { playStyle = it }
                3 -> ChoiceRow("Experience", Experience.entries, experience) { experience = it }
                4 -> ChoiceRow("Control style", ControlStyle.entries, controlStyle) { controlStyle = it }
                5 -> InfoCard("Starting settings", "The app seeds a balanced setup. You can edit every value after onboarding.")
                6 -> InfoCard("First setup ready", "The generator will combine device, profile, and calibration history into testable starting values.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Skip") { onDone(playStyle, experience, controlStyle) }
                if (step > 0) GhostButton("Back") { step-- }
                Button(
                    onClick = {
                        if (step == screens.lastIndex) onDone(playStyle, experience, controlStyle) else step++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
                ) {
                    Text(if (step == screens.lastIndex) "Create Profile" else "Next")
                }
            }
        }
    }
}

@Composable
private fun MainShell(vm: SensiViewModel) {
    val state = vm.state
    Scaffold(
        containerColor = Amoled,
        bottomBar = {
            NavigationBar(containerColor = Ink) {
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
                .background(Amoled)
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
                Tab.Tools -> item { ToolsScreen() }
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
        Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CoachPanel(coach: CoachState) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Mascot(coach.expression, modifier = Modifier.size(96.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coach", color = Cyan, fontWeight = FontWeight.Bold)
                Text(coach.message, color = Color.White)
            }
        }
    }
}

@Composable
private fun Mascot(expression: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )
    val eyeOffset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "eyes"
    )
    val glow by animateColorAsState(
        targetValue = when (expression) {
            "success", "happy", "excited" -> Lime
            "concerned", "warning", "confused" -> Amber
            "offline", "sleeping" -> TextMuted
            else -> Cyan
        },
        label = "glow"
    )
    Canvas(modifier = modifier) {
        val side = size.minDimension
        val bodySize = side * 0.72f * pulse
        val left = (size.width - bodySize) / 2
        val top = (size.height - bodySize) / 2
        drawCircle(glow.copy(alpha = 0.16f), radius = bodySize * 0.58f, center = center)
        drawRoundRect(
            brush = Brush.radialGradient(listOf(PanelSoft, Ink), center = center, radius = bodySize),
            topLeft = Offset(left, top),
            size = Size(bodySize, bodySize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodySize * 0.28f)
        )
        drawRoundRect(
            color = glow.copy(alpha = 0.8f),
            topLeft = Offset(left, top),
            size = Size(bodySize, bodySize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodySize * 0.28f),
            style = Stroke(width = 2.dp.toPx())
        )
        val eyeY = top + bodySize * 0.42f
        val xShift = when (expression) {
            "thinking", "analyzing", "focused" -> eyeOffset
            "confused" -> -eyeOffset
            else -> 0f
        }
        val eyeHeight = if (expression == "sleeping") 2.dp.toPx() else bodySize * 0.13f
        drawRoundRect(
            color = glow,
            topLeft = Offset(left + bodySize * 0.28f + xShift, eyeY),
            size = Size(bodySize * 0.16f, eyeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeHeight)
        )
        drawRoundRect(
            color = glow,
            topLeft = Offset(left + bodySize * 0.58f + xShift, eyeY),
            size = Size(bodySize * 0.16f, eyeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeHeight)
        )
        if (expression in setOf("success", "happy", "excited")) {
            val path = Path().apply {
                moveTo(left + bodySize * 0.34f, top + bodySize * 0.66f)
                quadraticBezierTo(center.x, top + bodySize * 0.82f, left + bodySize * 0.68f, top + bodySize * 0.66f)
            }
            drawPath(path, glow, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

@Composable
private fun HomeScreen(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatGrid(state)
        InfoCard("Core loop", "Device -> Profile -> Generate -> Test -> Analyze -> Adjust -> Retest -> Save -> Improve")
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
        Button(onClick = onRegenerate, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)) {
            Text("Generate Personalized Setup")
        }
        state.recommendations.forEach { item ->
            CardShell {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.scope.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${item.value}  |  range ${item.range.first}-${item.range.last}", color = Cyan)
                        Text("${item.confidence}% confidence - ${item.direction}", color = TextMuted)
                        Text(item.explanation, color = TextMuted)
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
        ChoiceRow("Scope", ScopeType.entries, scope) { scope = it }
        val value = state.profile.current.valueFor(scope)
        InfoCard("Current ${scope.label}", "$value. Run one test and choose the actual result.")
        Slider(value = value.toFloat(), onValueChange = {}, valueRange = 0f..100f, enabled = false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Overshoot") { onSignal(scope, CalibrationSignal.Overshoot) }
            GhostButton("Good") { onSignal(scope, CalibrationSignal.Good) }
            GhostButton("Undershoot") { onSignal(scope, CalibrationSignal.Undershoot) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Shaky") { onSignal(scope, CalibrationSignal.Shaky) }
            GhostButton("Slow") { onSignal(scope, CalibrationSignal.Slow) }
        }
        HistoryList(state.history)
    }
}

@Composable
private fun ToolsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolCard("Creators & Pros", "Source-gated reference presets. No fabricated creator settings.")
        ToolCard("Weapon Lab", "Close, mid, long-range and scope guidance for manual practice.")
        ToolCard("HUD Center", "2, 3, and 4-finger layout references. Does not modify the game HUD.")
        CrosshairPreview()
        ToolCard("DPI Center", "Explains DPI and sensitivity relationship. Does not change system DPI.")
        ToolCard("Device Center", "Automatic device facts plus verified/community distinction.")
        ToolCard("Performance Center", "Legitimate device diagnostics only. No fake boost claims.")
        ToolCard("Network Center", "Connection checks without pretending to measure game-server latency.")
        ToolCard("Guides", "Sensitivity, scopes, HUD, DPI, calibration, performance, and network lessons.")
        ToolCard("Community", "Post-MVP: moderation, reports, ratings, and sync required before upload.")
    }
}

@Composable
private fun ProfilesScreen(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardShell {
            Text(state.profile.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${state.profile.playStyle.label} / ${state.profile.experience.label} / ${state.profile.controlStyle.label}", color = TextMuted)
            Text("Weapon preference: ${state.profile.preferredWeapon}", color = TextMuted)
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
        Text(label, color = TextMuted)
        Text(value, color = Cyan, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DeviceCard() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val text = "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${config.screenWidthDp}x${config.screenHeightDp}dp"
    InfoCard("Detected device", text.ifBlank { context.packageName })
}

@Composable
private fun <T> ChoiceRow(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
        values.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val text = when (item) {
                        is PlayStyle -> item.label
                        is Experience -> item.label
                        is ControlStyle -> item.label
                        is ScopeType -> item.label
                        else -> item.toString()
                    }
                    val active = item == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (active) Cyan else PanelSoft, RoundedCornerShape(12.dp))
                            .background(if (active) Cyan.copy(alpha = 0.16f) else Panel, RoundedCornerShape(12.dp))
                            .clickable { onSelect(item) }
                            .padding(12.dp)
                    ) {
                        Text(text, color = if (active) Cyan else Color.White)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    CardShell {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, color = TextMuted)
    }
}

@Composable
private fun ToolCard(title: String, body: String) {
    InfoCard(title, body)
}

@Composable
private fun CrosshairPreview() {
    CardShell {
        Text("Crosshair Lab", color = Color.White, fontWeight = FontWeight.Bold)
        Text("In-app preview, save, duplicate, rename, export. No overlay in MVP.", color = TextMuted)
        Spacer(Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Ink, RoundedCornerShape(12.dp))
        ) {
            val c = center
            drawCircle(Cyan.copy(alpha = 0.15f), 34.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            drawCircle(Cyan, 3.dp.toPx(), c)
            drawLine(Cyan, Offset(c.x - 46.dp.toPx(), c.y), Offset(c.x - 14.dp.toPx(), c.y), strokeWidth = 3.dp.toPx())
            drawLine(Cyan, Offset(c.x + 14.dp.toPx(), c.y), Offset(c.x + 46.dp.toPx(), c.y), strokeWidth = 3.dp.toPx())
            drawLine(Cyan, Offset(c.x, c.y - 46.dp.toPx()), Offset(c.x, c.y - 14.dp.toPx()), strokeWidth = 3.dp.toPx())
            drawLine(Cyan, Offset(c.x, c.y + 14.dp.toPx()), Offset(c.x, c.y + 46.dp.toPx()), strokeWidth = 3.dp.toPx())
        }
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
          "freeLook": ${profile.current.freeLook}
        }
    """.trimIndent()
    CardShell {
        Text("Safe Export Preview", color = Color.White, fontWeight = FontWeight.Bold)
        Text(export, color = TextMuted)
    }
}

@Composable
private fun HistoryList(history: List<String>) {
    CardShell {
        Text("Improvement History", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (history.isEmpty()) {
            Text("No calibration changes yet.", color = TextMuted)
        } else {
            history.takeLast(6).forEach { Text("- $it", color = TextMuted) }
        }
    }
}

@Composable
private fun CardShell(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun GhostButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PanelSoft, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label)
    }
}

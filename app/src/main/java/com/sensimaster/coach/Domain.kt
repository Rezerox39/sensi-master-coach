package com.sensimaster.coach

import kotlin.math.roundToInt

enum class PlayStyle(val label: String) {
    Balanced("Balanced"),
    Rush("Rush"),
    Sniper("Sniper"),
    Shotgun("Shotgun"),
}

enum class Experience(val label: String) {
    Beginner("Beginner"),
    Intermediate("Intermediate"),
    Advanced("Advanced"),
}

enum class ControlStyle(val label: String) {
    TwoFinger("2-finger"),
    ThreeFinger("3-finger"),
    FourFinger("4-finger"),
}

enum class Motivation(val label: String) {
    ImproveAim("Improve aim"),
    RankPush("Rank push"),
    RecoilControl("Recoil control"),
    ClutchWins("Clutch wins"),
    SmoothTracking("Smooth tracking"),
}

enum class ScopeType(val label: String) {
    General("General"),
    RedDot("Red Dot"),
    TwoX("2x"),
    FourX("4x"),
    Sniper("AWM/Sniper"),
    FreeLook("Free Look"),
}

enum class CalibrationSignal {
    Good,
    Overshoot,
    Undershoot,
    Shaky,
    Slow,
}

data class SensitivitySet(
    val general: Int,
    val redDot: Int,
    val twoX: Int,
    val fourX: Int,
    val sniper: Int,
    val freeLook: Int,
) {
    fun valueFor(scope: ScopeType): Int = when (scope) {
        ScopeType.General -> general
        ScopeType.RedDot -> redDot
        ScopeType.TwoX -> twoX
        ScopeType.FourX -> fourX
        ScopeType.Sniper -> sniper
        ScopeType.FreeLook -> freeLook
    }

    fun withValue(scope: ScopeType, value: Int): SensitivitySet {
        val clamped = value.coerceIn(0, 100)
        return when (scope) {
            ScopeType.General -> copy(general = clamped)
            ScopeType.RedDot -> copy(redDot = clamped)
            ScopeType.TwoX -> copy(twoX = clamped)
            ScopeType.FourX -> copy(fourX = clamped)
            ScopeType.Sniper -> copy(sniper = clamped)
            ScopeType.FreeLook -> copy(freeLook = clamped)
        }
    }
}

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val refreshRate: Int,
    val width: Int,
    val height: Int,
    val dpi: Int = 0,
)

data class PlayerProfile(
    val name: String,
    val playStyle: PlayStyle,
    val experience: Experience,
    val controlStyle: ControlStyle,
    val preferredWeapon: String,
    val current: SensitivitySet,
    val device: DeviceSnapshot,
    val motivations: List<Motivation> = emptyList(),
)

data class ScopeRecommendation(
    val scope: ScopeType,
    val value: Int,
    val range: IntRange,
    val confidence: Int,
    val direction: String,
    val explanation: String,
)

data class CoachState(
    val expression: String,
    val message: String,
)

object RecommendationEngine {
    fun generate(profile: PlayerProfile, history: List<CalibrationSignal> = emptyList()): List<ScopeRecommendation> {
        return ScopeType.entries.map { scope ->
            val current = profile.current.valueFor(scope)
            val base = baseline(scope, profile)
            val historyOffset = historyOffset(history)
            val suggested = blend(current, base + historyOffset)
            val range = (suggested - 5).coerceAtLeast(0)..(suggested + 5).coerceAtMost(100)
            val confidence = confidence(profile, history)
            ScopeRecommendation(
                scope = scope,
                value = suggested,
                range = range,
                confidence = confidence,
                direction = direction(current, suggested),
                explanation = explanation(scope, profile, suggested)
            )
        }
    }

    private fun baseline(scope: ScopeType, profile: PlayerProfile): Int {
        val style = when (profile.playStyle) {
            PlayStyle.Rush -> 6
            PlayStyle.Shotgun -> 4
            PlayStyle.Balanced -> 0
            PlayStyle.Sniper -> -5
        }
        val experience = when (profile.experience) {
            Experience.Beginner -> -4
            Experience.Intermediate -> 0
            Experience.Advanced -> 3
        }
        val refresh = if (profile.device.refreshRate >= 90) -2 else 1
        val control = when (profile.controlStyle) {
            ControlStyle.TwoFinger -> -2
            ControlStyle.ThreeFinger -> 0
            ControlStyle.FourFinger -> 2
        }
        val dpi = when {
            profile.device.dpi >= 500 -> -3
            profile.device.dpi in 400..499 -> -1
            profile.device.dpi in 300..399 -> 2
            profile.device.dpi in 1..299 -> 3
            else -> 0
        }
        val scopeBase = when (scope) {
            ScopeType.General -> 88
            ScopeType.RedDot -> 82
            ScopeType.TwoX -> 74
            ScopeType.FourX -> 64
            ScopeType.Sniper -> 48
            ScopeType.FreeLook -> 70
        }
        return (scopeBase + style + experience + refresh + control + dpi).coerceIn(20, 98)
    }

    private fun historyOffset(history: List<CalibrationSignal>): Int {
        val recent = history.takeLast(4)
        return recent.sumOf {
            when (it) {
                CalibrationSignal.Overshoot, CalibrationSignal.Shaky -> -2
                CalibrationSignal.Undershoot, CalibrationSignal.Slow -> 2
                CalibrationSignal.Good -> 0
            }
        }.coerceIn(-6, 6)
    }

    private fun blend(current: Int, target: Int): Int = (current * 0.45f + target * 0.55f).roundToInt().coerceIn(0, 100)

    private fun confidence(profile: PlayerProfile, history: List<CalibrationSignal>): Int {
        var value = 58
        if (profile.device.refreshRate > 0) value += 8
        if (profile.current.general > 0) value += 8
        value += history.size.coerceAtMost(5) * 4
        return value.coerceIn(45, 92)
    }

    private fun direction(current: Int, suggested: Int): String = when {
        suggested > current + 2 -> "Increase gradually"
        suggested < current - 2 -> "Decrease gradually"
        else -> "Test before changing"
    }

    private fun explanation(scope: ScopeType, profile: PlayerProfile, suggested: Int): String {
        val pace = if (profile.playStyle == PlayStyle.Rush || profile.playStyle == PlayStyle.Shotgun) {
            "faster close-range correction"
        } else {
            "steadier target control"
        }
        return "${scope.label} starts at $suggested for ${profile.device.model}, ${profile.controlStyle.label}, and $pace."
    }
}

object CalibrationEngine {
    fun adjust(current: SensitivitySet, scope: ScopeType, signal: CalibrationSignal): Pair<SensitivitySet, String> {
        val oldValue = current.valueFor(scope)
        val delta = when (signal) {
            CalibrationSignal.Good -> 0
            CalibrationSignal.Overshoot -> -3
            CalibrationSignal.Undershoot -> 3
            CalibrationSignal.Shaky -> -2
            CalibrationSignal.Slow -> 2
        }
        val next = current.withValue(scope, oldValue + delta)
        val message = when (signal) {
            CalibrationSignal.Good -> "Keep ${scope.label} at $oldValue and retest for consistency."
            CalibrationSignal.Overshoot -> "Lower ${scope.label} by 3. Retest before changing another value."
            CalibrationSignal.Undershoot -> "Raise ${scope.label} by 3. Retest before changing another value."
            CalibrationSignal.Shaky -> "Lower ${scope.label} slightly and focus on smoother tracking."
            CalibrationSignal.Slow -> "Raise ${scope.label} slightly and retest reaction timing."
        }
        return next to message
    }
}

fun defaultProfile(device: DeviceSnapshot): PlayerProfile = PlayerProfile(
    name = "Main",
    playStyle = PlayStyle.Balanced,
    experience = Experience.Intermediate,
    controlStyle = ControlStyle.ThreeFinger,
    preferredWeapon = "Balanced AR + SMG",
    motivations = listOf(Motivation.ImproveAim, Motivation.SmoothTracking),
    current = SensitivitySet(
        general = 85,
        redDot = 80,
        twoX = 72,
        fourX = 62,
        sniper = 45,
        freeLook = 68
    ),
    device = device
)

// Crosshair domain -----------------------------------------------------------

enum class CrosshairShape(val label: String) {
    Cross("Cross"),
    Dot("Dot"),
    Circle("Circle"),
    Split("Split"),
    TShape("T"),
}

enum class CrosshairColor(val label: String, val argb: Long) {
    White("White", 0xFFFFFFFF),
    Red("Red", 0xFFFF1F1F),
    Green("Green", 0xFF69F0AE),
    Cyan("Cyan", 0xFF00E5FF),
    Yellow("Yellow", 0xFFFFD700),
    Magenta("Magenta", 0xFFFF3D9A),
}

data class CrosshairConfig(
    val shape: CrosshairShape = CrosshairShape.Cross,
    val color: CrosshairColor = CrosshairColor.Red,
    val size: Int = 56,
    val thickness: Int = 4,
    val gap: Int = 8,
    val opacity: Float = 1f,
    val centerDot: Boolean = true,
    val dotRadius: Int = 5,
    val outline: Boolean = true,
) {
    init {
        require(size in 16..120) { "size out of range" }
        require(thickness in 1..12) { "thickness out of range" }
        require(gap in 0..40) { "gap out of range" }
        require(opacity in 0.2f..1f) { "opacity out of range" }
        require(dotRadius in 2..14) { "dotRadius out of range" }
    }
}

object CrosshairPresets {
    val cross = CrosshairConfig()
    val dot = CrosshairConfig(
        shape = CrosshairShape.Dot,
        color = CrosshairColor.White,
        size = 20,
        thickness = 5,
        gap = 0,
        centerDot = true,
        dotRadius = 7,
        outline = true
    )
    val circle = CrosshairConfig(
        shape = CrosshairShape.Circle,
        color = CrosshairColor.Green,
        size = 72,
        thickness = 4,
        gap = 0,
        centerDot = true,
        dotRadius = 4,
        outline = true
    )
    val split = CrosshairConfig(
        shape = CrosshairShape.Split,
        color = CrosshairColor.Cyan,
        size = 72,
        thickness = 4,
        gap = 10,
        centerDot = true,
        dotRadius = 4,
        outline = true
    )
    val tShape = CrosshairConfig(
        shape = CrosshairShape.TShape,
        color = CrosshairColor.Yellow,
        size = 64,
        thickness = 4,
        gap = 8,
        centerDot = false,
        dotRadius = 4,
        outline = true
    )

    val series = listOf(cross, dot, circle, split, tShape)
}

fun crosshairName(index: Int): String = when (index) {
    0 -> "Classic Red"
    1 -> "Tiny Dot"
    2 -> "Green Ring"
    3 -> "Split Cyan"
    4 -> "Gold T"
    else -> "Saved $index"
}

fun crosshairDescription(config: CrosshairConfig): String = buildString {
    appendLine("Crosshair preset")
    appendLine("Shape: ${config.shape.label}")
    appendLine("Color: ${config.color.label}")
    appendLine("Size: ${config.size}px  Thickness: ${config.thickness}px  Gap: ${config.gap}px")
    appendLine("Opacity: ${(config.opacity * 100).toInt()}%")
    appendLine("Center dot: ${if (config.centerDot) "on (${config.dotRadius}px)" else "off"}")
    appendLine("Outline: ${if (config.outline) "on" else "off"}")
    appendLine("Apply manually in-game; this app does not overlay or modify the game.")
}

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
)

data class PlayerProfile(
    val name: String,
    val playStyle: PlayStyle,
    val experience: Experience,
    val controlStyle: ControlStyle,
    val preferredWeapon: String,
    val current: SensitivitySet,
    val device: DeviceSnapshot,
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
        val scopeBase = when (scope) {
            ScopeType.General -> 88
            ScopeType.RedDot -> 82
            ScopeType.TwoX -> 74
            ScopeType.FourX -> 64
            ScopeType.Sniper -> 48
            ScopeType.FreeLook -> 70
        }
        return (scopeBase + style + experience + refresh + control).coerceIn(20, 98)
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

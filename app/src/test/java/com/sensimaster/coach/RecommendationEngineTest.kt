package com.sensimaster.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val device = DeviceSnapshot(
        manufacturer = "Test",
        model = "Device",
        androidVersion = "15",
        refreshRate = 120,
        width = 1080,
        height = 2400
    )

    @Test
    fun recommendationsIncludeAllScopesWithValidRanges() {
        val profile = defaultProfile(device)

        val result = RecommendationEngine.generate(profile)

        assertEquals(ScopeType.entries.size, result.size)
        result.forEach {
            assertTrue(it.value in 0..100)
            assertTrue(it.range.first in 0..100)
            assertTrue(it.range.last in 0..100)
            assertTrue(it.confidence in 45..92)
        }
    }

    @Test
    fun rushProfileProducesHigherGeneralThanSniperProfile() {
        val base = defaultProfile(device)
        val rush = base.copy(playStyle = PlayStyle.Rush)
        val sniper = base.copy(playStyle = PlayStyle.Sniper)

        val rushGeneral = RecommendationEngine.generate(rush).first { it.scope == ScopeType.General }.value
        val sniperGeneral = RecommendationEngine.generate(sniper).first { it.scope == ScopeType.General }.value

        assertTrue(rushGeneral > sniperGeneral)
    }

    @Test
    fun overshootLowersOnlySelectedScope() {
        val current = defaultProfile(device).current

        val (next, message) = CalibrationEngine.adjust(current, ScopeType.RedDot, CalibrationSignal.Overshoot)

        assertEquals(current.redDot - 3, next.redDot)
        assertEquals(current.general, next.general)
        assertEquals(current.twoX, next.twoX)
        assertTrue(message.contains("Retest"))
    }

    @Test
    fun goodCalibrationKeepsValue() {
        val current = defaultProfile(device).current

        val (next, _) = CalibrationEngine.adjust(current, ScopeType.Sniper, CalibrationSignal.Good)

        assertEquals(current, next)
    }
}

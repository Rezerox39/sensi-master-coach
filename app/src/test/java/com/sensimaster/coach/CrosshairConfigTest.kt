package com.sensimaster.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrosshairConfigTest {

    @Test
    fun presetsStayWithinAllowedRanges() {
        CrosshairPresets.series.forEach { preset ->
            assertTrue("size ${preset.size}", preset.size in 16..120)
            assertTrue("thickness ${preset.thickness}", preset.thickness in 1..12)
            assertTrue("gap ${preset.gap}", preset.gap in 0..40)
            assertTrue("opacity ${preset.opacity}", preset.opacity in 0.2f..1f)
            assertTrue("dotRadius ${preset.dotRadius}", preset.dotRadius in 2..14)
        }
    }

    @Test
    fun presetsCoverMultipleShapes() {
        val shapes = CrosshairPresets.series.map { it.shape }.toSet()
        assertEquals(CrosshairShape.entries.size, shapes.size)
    }

    @Test
    fun defaultIsRedCrossWithCenterDot() {
        val c = CrosshairPresets.cross
        assertEquals(CrosshairShape.Cross, c.shape)
        assertEquals(CrosshairColor.Red, c.color)
        assertTrue(c.centerDot)
        assertTrue(c.outline)
    }

    @Test
    fun descriptionMentionsManualApplication() {
        val text = crosshairDescription(CrosshairPresets.split)
        assertTrue(text.contains("Split"))
        assertTrue(text.contains("manual"))
        assertTrue(text.contains("Center dot"))
    }

    @Test
    fun invalidSizeIsRejected() {
        val valid = CrosshairPresets.cross
        var rejected = false
        try {
            valid.copy(size = 8)
        } catch (e: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertFalse(valid.size in 1..8)
    }
}

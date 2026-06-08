package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [HeightTool].
 */
class HeightToolTest {

    private val tool = HeightTool()
    private val TOLERANCE = 0.001f

    @Test
    fun calculate_height_groundToTop() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(0f, 2f, 0f)),
            "m"
        )
        assertEquals(2f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun calculate_height_autoDetectGroundAndTop() {
        // User taps top first, then ground — tool should auto-detect
        val result = tool.calculate(
            listOf(Point3D(0f, 5f, 0f), Point3D(0f, 0f, 0f)),
            "m"
        )
        assertEquals(5f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun lineSegments_showsVerticalAndHorizontal() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(0f, 3f, 0f))
        )
        assertEquals(2, segs.size) // horizontal base + vertical height
    }
}

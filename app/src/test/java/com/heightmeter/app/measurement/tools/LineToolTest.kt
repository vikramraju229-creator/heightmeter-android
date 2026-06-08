package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [LineTool].
 */
class LineToolTest {

    private val tool = LineTool()
    private val TOLERANCE = 0.001f

    @Test
    fun displayName() {
        assertEquals("Line", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(2, tool.minPoints)
        assertEquals(2, tool.maxPoints)
    }

    @Test
    fun instructions_initial() {
        assertEquals("Tap to place first point", tool.getInstructions(0))
    }

    @Test
    fun calculate_distance() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(3f, 4f, 0f)),
            "m"
        )
        assertEquals(5f, result.primaryValue, TOLERANCE)
        assertEquals(1, result.labels.size)
    }

    @Test
    fun lineSegments_twoPoints() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(1f, 1f, 1f))
        )
        assertEquals(1, segs.size)
    }

    @Test
    fun lineSegments_onePoint_returnsEmpty() {
        assert(tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty())
    }
}

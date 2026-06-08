package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [CubeTool].
 */
class CubeToolTest {

    private val tool = CubeTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Cube", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(2, tool.minPoints)
        assertEquals(2, tool.maxPoints)
    }

    @Test
    fun calculate_sideAndVolume() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(3f, 0f, 0f)),
            "m"
        )
        assertEquals(3f, result.primaryValue, TOLERANCE)   // side
        assertEquals(27f, result.secondaryValue, TOLERANCE) // volume
        assertEquals(8, result.points.size)                 // 8 corners
    }

    @Test
    fun getLineSegments_12Segments() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(2f, 0f, 0f))
        )
        // 4 bottom + 4 top + 4 vertical = 12
        assertEquals(12, segs.size)
    }

    @Test
    fun getLineSegments_onePoint_returnsEmpty() {
        assertTrue(
            tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty()
        )
    }
}

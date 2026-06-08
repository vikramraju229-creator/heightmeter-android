package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [DistanceTool].
 */
class DistanceToolTest {

    private val tool = DistanceTool()
    private val TOLERANCE = 0.001f

    @Test
    fun displayName() {
        assertEquals("Distance", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(2, tool.minPoints)
        assertEquals(2, tool.maxPoints)
    }

    @Test
    fun calculate_3dDistance() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(1f, 2f, 2f)),
            "m"
        )
        assertEquals(3f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun calculate_zeroDistance() {
        val result = tool.calculate(
            listOf(Point3D(1f, 1f, 1f), Point3D(1f, 1f, 1f)),
            "m"
        )
        assertEquals(0f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun getLineSegments_oneSegment() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(5f, 5f, 5f))
        )
        assertEquals(1, segs.size)
        assertEquals(Point3D(0f, 0f, 0f), segs[0].first)
        assertEquals(Point3D(5f, 5f, 5f), segs[0].second)
    }

    @Test
    fun getLineSegments_onePoint_returnsEmpty() {
        assertTrue(tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty())
    }
}

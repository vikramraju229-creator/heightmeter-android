package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [SquareTool].
 */
class SquareToolTest {

    private val tool = SquareTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Square", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(2, tool.minPoints)
        assertEquals(2, tool.maxPoints)
    }

    @Test
    fun calculate_sideLengthAndArea() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(3f, 0f, 0f)),
            "m"
        )
        assertEquals(3f, result.primaryValue, TOLERANCE)
        assertEquals(9f, result.secondaryValue, TOLERANCE)
        assertEquals(4, result.points.size)
    }

    @Test
    fun getSquareCorners_perpendicular() {
        val corners = tool.getSquareCorners(
            listOf(Point3D(0f, 0f, 0f), Point3D(2f, 0f, 0f))
        )
        assertEquals(4, corners.size)
        // First two should match input
        assertEquals(Point3D(0f, 0f, 0f), corners[0])
        assertEquals(Point3D(2f, 0f, 0f), corners[1])
        // Should be a square — side lengths equal
        val side1 = kotlin.math.sqrt(
            (corners[1].x - corners[0].x).let { it * it } +
            (corners[1].y - corners[0].y).let { it * it } +
            (corners[1].z - corners[0].z).let { it * it }
        )
        val side2 = kotlin.math.sqrt(
            (corners[2].x - corners[1].x).let { it * it } +
            (corners[2].y - corners[1].y).let { it * it } +
            (corners[2].z - corners[1].z).let { it * it }
        )
        assertEquals(side1, side2, TOLERANCE)
    }

    @Test
    fun getSquareCorners_verticalDirection_fallbackToX() {
        // Purely vertical direction should use X-axis fallback
        val corners = tool.getSquareCorners(
            listOf(Point3D(0f, 0f, 0f), Point3D(0f, 5f, 0f))
        )
        assertEquals(4, corners.size)
        // The perpendicular should be along X axis (1,0,0) * 5 = (5,0,0)
        assertEquals(Point3D(5f, 0f, 0f), corners[2])
        assertEquals(Point3D(5f, 0f, 0f), corners[3])
    }

    @Test
    fun getLineSegments_returns4Segments() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f))
        )
        assertEquals(4, segs.size)
        // Check closed loop
        assertEquals(segs.last().second, segs.first().first)
    }

    @Test
    fun getLineSegments_onePoint_returnsEmpty() {
        assertTrue(tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty())
    }
}

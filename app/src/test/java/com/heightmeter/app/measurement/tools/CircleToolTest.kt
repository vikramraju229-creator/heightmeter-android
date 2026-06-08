package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [CircleTool].
 */
class CircleToolTest {

    private val tool = CircleTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Circle", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(2, tool.minPoints)
        assertEquals(2, tool.maxPoints)
    }

    @Test
    fun calculate_radiusAndDiameter() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(2f, 0f, 0f)),
            "m"
        )
        assertEquals(2f, result.primaryValue, TOLERANCE)    // radius
        assertEquals(4f, result.secondaryValue, TOLERANCE)   // diameter
        // area = PI * r² = PI * 4
        val expectedArea = kotlin.math.PI.toFloat() * 4f
        assertEquals(expectedArea, result.tertiaryValue, TOLERANCE)
    }

    @Test
    fun calculate_verticalRadius() {
        val result = tool.calculate(
            listOf(Point3D(0f, 0f, 0f), Point3D(0f, 0f, 3f)),
            "m"
        )
        assertEquals(3f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun getLineSegments_centerToEdge() {
        val segs = tool.getLineSegments(
            listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f))
        )
        assertEquals(1, segs.size)
        assertEquals(Point3D(0f, 0f, 0f), segs[0].first)
        assertEquals(Point3D(1f, 0f, 0f), segs[0].second)
    }

    @Test
    fun getLineSegments_onePoint_returnsEmpty() {
        assertTrue(tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty())
    }

    @Test
    fun getCirclePoints_generatesCorrectCount() {
        val points = tool.getCirclePoints(
            center = Point3D(0f, 0f, 0f),
            radius = 1f,
            segments = 36
        )
        assertEquals(36, points.size)
    }

    @Test
    fun getCirclePoints_allSameY() {
        val points = tool.getCirclePoints(
            center = Point3D(0f, 2f, 0f),
            radius = 1f,
            segments = 8
        )
        for (p in points) {
            assertEquals(2f, p.y, TOLERANCE)
        }
    }

    @Test
    fun getCirclePoints_firstPointAtZeroDegrees() {
        val points = tool.getCirclePoints(
            center = Point3D(0f, 0f, 0f),
            radius = 2f,
            segments = 4
        )
        // At i=0, angle=0: x = 2*cos(0) = 2, z = 2*sin(0) = 0
        assertEquals(Point3D(2f, 0f, 0f), points[0])
    }
}

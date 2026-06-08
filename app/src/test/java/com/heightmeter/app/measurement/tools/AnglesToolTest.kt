package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [AnglesTool].
 */
class AnglesToolTest {

    private val tool = AnglesTool()
    private val TOLERANCE = 0.1f

    @Test
    fun displayName() {
        assertEquals("Angles", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(3, tool.minPoints)
        assertEquals(3, tool.maxPoints)
    }

    @Test
    fun calculate_rightAngle() {
        val result = tool.calculate(
            listOf(
                Point3D(1f, 0f, 0f),  // A
                Point3D(0f, 0f, 0f),  // B (vertex)
                Point3D(0f, 0f, 1f)   // C
            ),
            "m"
        )
        assertEquals(90f, result.primaryValue, TOLERANCE)
        assertEquals(3, result.points.size)
    }

    @Test
    fun calculate_straightAngle() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(2f, 0f, 0f)
            ),
            "m"
        )
        assertEquals(180f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun calculate_acuteAngle() {
        val result = tool.calculate(
            listOf(
                Point3D(1f, 0f, 0f),
                Point3D(0f, 0f, 0f),
                Point3D(1f, 1f, 0f)
            ),
            "m"
        )
        // 45-degree angle
        assertEquals(45f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun getLineSegments_2Segments() {
        val segs = tool.getLineSegments(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(2f, 0f, 0f)
            )
        )
        assertEquals(2, segs.size)
    }

    @Test
    fun getLineSegments_fewerThan3_returnsEmpty() {
        assertTrue(
            tool.getLineSegments(listOf(Point3D(0f, 0f, 0f))).isEmpty()
        )
    }
}

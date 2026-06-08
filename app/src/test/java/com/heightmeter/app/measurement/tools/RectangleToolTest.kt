package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [RectangleTool].
 */
class RectangleToolTest {

    private val tool = RectangleTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Rectangle", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(3, tool.minPoints)
        assertEquals(3, tool.maxPoints)
    }

    @Test
    fun calculate_widthHeightAndArea() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),      // first corner
                Point3D(3f, 0f, 0f),      // second corner (width=3)
                Point3D(0f, 0f, 4f)       // width direction (height=4)
            ),
            "m"
        )
        assertEquals(3f, result.primaryValue, TOLERANCE)   // width
        assertEquals(4f, result.secondaryValue, TOLERANCE)  // height
        assertEquals(12f, result.tertiaryValue, TOLERANCE) // area
        assertEquals(4, result.points.size)
    }

    @Test
    fun getRectangleCorners_returns4() {
        val corners = tool.getRectangleCorners(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(2f, 0f, 0f),
                Point3D(0f, 0f, 3f)
            )
        )
        assertEquals(4, corners.size)
        // Check perpendicular - third point at z=3 should give width in z
        assertEquals(Point3D(0f, 0f, 0f), corners[0])
        assertEquals(Point3D(2f, 0f, 0f), corners[1])
        // corner3 should be (2, 0, 3)
        assertEquals(2f, corners[2].x, TOLERANCE)
        assertEquals(3f, corners[2].z, TOLERANCE)
        // corner4 should be (0, 0, 3)
        assertEquals(0f, corners[3].x, TOLERANCE)
        assertEquals(3f, corners[3].z, TOLERANCE)
    }

    @Test
    fun getLineSegments_returnsEmpty() {
        // Rectangle tool handles segments via calculate()
        assertTrue(
            tool.getLineSegments(
                listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f), Point3D(0f, 0f, 1f))
            ).isEmpty()
        )
    }
}

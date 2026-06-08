package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [CuboidTool].
 */
class CuboidToolTest {

    private val tool = CuboidTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Cuboid", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(3, tool.minPoints)
        assertEquals(3, tool.maxPoints)
    }

    @Test
    fun calculate_volume_2x3x4() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),   // first base corner
                Point3D(2f, 0f, 3f),   // opposite base corner (width=2, depth=3)
                Point3D(0f, 4f, 0f)    // height point (height=4)
            ),
            "m"
        )
        // volume = 2 * 4 * 3 = 24
        assertEquals(24f, result.primaryValue, TOLERANCE)
        assertEquals(2f, result.secondaryValue, TOLERANCE)  // width
        assertEquals(4f, result.tertiaryValue, TOLERANCE)   // height
        assertEquals(8, result.points.size)
    }

    @Test
    fun calculate_volume_zeroHeight() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(2f, 0f, 2f),
                Point3D(0f, 0f, 0f)    // zero height
            ),
            "m"
        )
        assertEquals(0f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun getLineSegments_12EdgeSegments() {
        val segs = tool.getLineSegments(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(2f, 0f, 3f),
                Point3D(0f, 4f, 0f)
            )
        )
        // 4 bottom + 4 top + 4 vertical = 12
        assertEquals(12, segs.size)
    }

    @Test
    fun getLineSegments_fewerThan3_returnsEmpty() {
        assertTrue(
            tool.getLineSegments(
                listOf(Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f))
            ).isEmpty()
        )
    }
}

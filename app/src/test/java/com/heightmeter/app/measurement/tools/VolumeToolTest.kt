package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [VolumeTool].
 */
class VolumeToolTest {

    private val tool = VolumeTool()
    private val TOLERANCE = 0.01f

    @Test
    fun displayName() {
        assertEquals("Volume", tool.displayName)
    }

    @Test
    fun minMaxPoints() {
        assertEquals(5, tool.minPoints)
        assertEquals(5, tool.maxPoints)
    }

    @Test
    fun calculate_extrudedVolume_unitSquare() {
        // 4 base corners (unit square at y=0) + height point at y=3
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(1f, 0f, 1f),
                Point3D(0f, 0f, 1f),
                Point3D(0f, 3f, 0f)  // height point
            ),
            "m"
        )
        // base area = 1, height = 3, volume = 3
        assertEquals(3f, result.primaryValue, TOLERANCE)
        assertEquals(1f, result.secondaryValue, TOLERANCE)  // base area
        assertEquals(3f, result.tertiaryValue, TOLERANCE)   // height
        // points: 4 base + 4 top + 1 height = 9
        assertEquals(9, result.points.size)
    }

    @Test
    fun calculate_extrudedVolume_large() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(2f, 0f, 0f),
                Point3D(2f, 0f, 2f),
                Point3D(0f, 0f, 2f),
                Point3D(0f, 5f, 0f)
            ),
            "m"
        )
        // area = 4, height = 5, volume = 20
        assertEquals(20f, result.primaryValue, TOLERANCE)
    }

    @Test
    fun getLineSegments_14Segments() {
        val segs = tool.getLineSegments(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(1f, 0f, 1f),
                Point3D(0f, 0f, 1f),
                Point3D(0f, 3f, 0f)
            )
        )
        // 4 base + 4 top + 4 vertical + 1 height = 13
        assertEquals(13, segs.size)
    }

    @Test
    fun getLineSegments_fewerThan5_returnsEmpty() {
        assertTrue(
            tool.getLineSegments(
                listOf(
                    Point3D(0f, 0f, 0f),
                    Point3D(1f, 0f, 0f)
                )
            ).isEmpty()
        )
    }
}

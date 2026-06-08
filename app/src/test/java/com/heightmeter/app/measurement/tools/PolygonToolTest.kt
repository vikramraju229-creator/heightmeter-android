package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [PolygonTool].
 */
class PolygonToolTest {

    private val tool = PolygonTool()
    private val TOLERANCE = 0.001f

    @Test
    fun calculate_area_unitSquare() {
        val result = tool.calculate(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(1f, 0f, 1f)
            ),
            "m"
        )
        // Triangle area of unit square half = 0.5
        assertEquals(0.5f, result.secondaryValue!!, TOLERANCE)
    }

    @Test
    fun lineSegments_closed() {
        val segs = tool.getLineSegments(
            listOf(
                Point3D(0f, 0f, 0f),
                Point3D(1f, 0f, 0f),
                Point3D(1f, 0f, 1f)
            )
        )
        assertEquals(3, segs.size)
        // Last segment should close the polygon
        assertEquals(segs.last().first, segs[1].second) // (1,0,1) -> (0,0,0)
        assertEquals(segs.last().second, segs.first().first)
    }
}

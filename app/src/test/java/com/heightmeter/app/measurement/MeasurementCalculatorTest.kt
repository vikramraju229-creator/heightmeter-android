package com.heightmeter.app.measurement

import com.heightmeter.app.measurement.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [MeasurementCalculator].
 * These are pure math — no Android dependencies, fast to run.
 */
class MeasurementCalculatorTest {

    private val TOLERANCE = 0.001f

    // ── 2D Distance ──────────────────────────────────────────

    @Test
    fun distance2D_horizontal() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(3f, 5f, 4f)
        assertEquals(5f, MeasurementCalculator.distance2D(a, b), TOLERANCE)
    }

    @Test
    fun distance2D_zero() {
        val a = Point3D(1f, 2f, 3f)
        assertEquals(0f, MeasurementCalculator.distance2D(a, a), TOLERANCE)
    }

    // ── 3D Distance ──────────────────────────────────────────

    @Test
    fun distance3D_standard() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(1f, 2f, 2f)
        assertEquals(3f, MeasurementCalculator.distance3D(a, b), TOLERANCE)
    }

    @Test
    fun distance3D_negative_coords() {
        val a = Point3D(-1f, -1f, -1f)
        val b = Point3D(1f, 1f, 1f)
        val expected = kotlin.math.sqrt(12f) // 2^2 + 2^2 + 2^2 = 12
        assertEquals(expected, MeasurementCalculator.distance3D(a, b), TOLERANCE)
    }

    // ── Angle ────────────────────────────────────────────────

    @Test
    fun angleBetween_rightAngle() {
        val a = Point3D(1f, 0f, 0f)
        val b = Point3D(0f, 0f, 0f) // vertex
        val c = Point3D(0f, 0f, 1f)
        assertEquals(90f, MeasurementCalculator.angleBetween(a, b, c), TOLERANCE)
    }

    @Test
    fun angleBetween_straightLine() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(1f, 0f, 0f)
        val c = Point3D(2f, 0f, 0f)
        assertEquals(180f, MeasurementCalculator.angleBetween(a, b, c), TOLERANCE)
    }

    @Test
    fun angleBetween_zeroLength() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(0f, 0f, 0f) // zero-length edge
        val c = Point3D(1f, 0f, 0f)
        assertEquals(0f, MeasurementCalculator.angleBetween(a, b, c), TOLERANCE)
    }

    // ── Triangle Area ────────────────────────────────────────

    @Test
    fun triangleArea_standard() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(3f, 0f, 0f)
        val c = Point3D(0f, 0f, 4f)
        assertEquals(6f, MeasurementCalculator.triangleArea(a, b, c), TOLERANCE)
    }

    @Test
    fun triangleArea_collinear_returnsZero() {
        val a = Point3D(0f, 0f, 0f)
        val b = Point3D(1f, 0f, 0f)
        val c = Point3D(2f, 0f, 0f)
        assertTrue(MeasurementCalculator.triangleArea(a, b, c) < 0.001f)
    }

    // ── Polygon Area (THE ONE THAT WAS BROKEN) ───────────────

    @Test
    fun polygonArea_unitSquare_xyPlane() {
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(1f, 0f, 0f),
            Point3D(1f, 0f, 1f),
            Point3D(0f, 0f, 1f)
        )
        assertEquals(1.0f, MeasurementCalculator.polygonArea(square), TOLERANCE)
    }

    @Test
    fun polygonArea_largeSquare() {
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(5f, 0f, 0f),
            Point3D(5f, 0f, 5f),
            Point3D(0f, 0f, 5f)
        )
        assertEquals(25.0f, MeasurementCalculator.polygonArea(square), TOLERANCE)
    }

    @Test
    fun polygonArea_triangle() {
        val triangle = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(3f, 0f, 0f),
            Point3D(0f, 0f, 4f)
        )
        assertEquals(6.0f, MeasurementCalculator.polygonArea(triangle), TOLERANCE)
    }

    @Test
    fun polygonArea_verticalPlane() {
        // Square on a vertical plane (XZ rotated to XY)
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(0f, 1f, 0f),
            Point3D(0f, 1f, 1f),
            Point3D(0f, 0f, 1f)
        )
        assertEquals(1.0f, MeasurementCalculator.polygonArea(square), TOLERANCE)
    }

    @Test
    fun polygonArea_fewerThan3Points_returnsZero() {
        assertEquals(0f, MeasurementCalculator.polygonArea(listOf(Point3D(0f, 0f, 0f))), TOLERANCE)
        assertEquals(0f, MeasurementCalculator.polygonArea(emptyList()), TOLERANCE)
    }

    // ── Perimeter ────────────────────────────────────────────

    @Test
    fun polygonPerimeter_square() {
        val square = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(1f, 0f, 0f),
            Point3D(1f, 0f, 1f),
            Point3D(0f, 0f, 1f)
        )
        assertEquals(4f, MeasurementCalculator.polygonPerimeter(square), TOLERANCE)
    }

    @Test
    fun polygonPerimeter_fewerThan2_returnsZero() {
        assertEquals(0f, MeasurementCalculator.polygonPerimeter(listOf(Point3D(0f, 0f, 0f))), TOLERANCE)
    }

    // ── Volume ───────────────────────────────────────────────

    @Test
    fun cuboidVolume() {
        assertEquals(24f, MeasurementCalculator.cuboidVolume(2f, 3f, 4f), TOLERANCE)
    }

    @Test
    fun cubeVolume() {
        assertEquals(27f, MeasurementCalculator.cubeVolume(3f), TOLERANCE)
    }

    @Test
    fun cylinderVolume() {
        val expected = kotlin.math.PI.toFloat() * 4f * 5f // π * r² * h
        assertEquals(expected, MeasurementCalculator.cylinderVolume(2f, 5f), TOLERANCE)
    }

    // ── Geometry Helpers ─────────────────────────────────────

    @Test
    fun midpoint() {
        val m = MeasurementCalculator.midpoint(Point3D(0f, 0f, 0f), Point3D(2f, 4f, 6f))
        assertEquals(Point3D(x = 1f, y = 2f, z = 3f), m)
    }

    @Test
    fun centroid_threePoints() {
        val c = MeasurementCalculator.centroid(listOf(
            Point3D(0f, 0f, 0f),
            Point3D(2f, 0f, 0f),
            Point3D(0f, 0f, 2f)
        ))
        assertEquals(Point3D(x = 2f / 3f, y = 0f, z = 2f / 3f), c)
    }

    @Test
    fun centroid_empty_returnsZero() {
        assertEquals(Point3D(x = 0f, y = 0f, z = 0f), MeasurementCalculator.centroid(emptyList()))
    }

    @Test
    fun verticalHeight() {
        val h = MeasurementCalculator.verticalHeight(
            Point3D(0f, 0f, 0f),
            Point3D(0f, 5f, 0f)
        )
        assertEquals(5f, h, TOLERANCE)
    }

    @Test
    fun direction_normalized() {
        val d = MeasurementCalculator.direction(
            Point3D(0f, 0f, 0f),
            Point3D(3f, 4f, 0f)
        )
        assertEquals(Point3D(x = 0.6f, y = 0.8f, z = 0f), d)
    }

    @Test
    fun direction_zeroLength_returnsZero() {
        val d = MeasurementCalculator.direction(
            Point3D(1f, 1f, 1f),
            Point3D(1f, 1f, 1f)
        )
        assertEquals(Point3D(x = 0f, y = 0f, z = 0f), d)
    }

    // ── Formatting ───────────────────────────────────────────

    @Test
    fun formatValue_meters() {
        assertEquals("1.50 m", MeasurementCalculator.formatValue(1.5f, "m"))
    }

    @Test
    fun formatValue_feet() {
        assertEquals("3.28 ft", MeasurementCalculator.formatValue(1f, "ft"))
    }

    @Test
    fun formatValue_centimeters() {
        assertEquals("150.0 cm", MeasurementCalculator.formatValue(1.5f, "cm"))
    }

    @Test
    fun formatValue_inches() {
        assertEquals("59.1 in", MeasurementCalculator.formatValue(1.5f, "in"))
    }

    @Test
    fun formatAngle() {
        assertEquals("45.0°", MeasurementCalculator.formatAngle(45f))
    }

    @Test
    fun formatArea_squareMeters() {
        assertEquals("2.50 m²", MeasurementCalculator.formatArea(2.5f, "m"))
    }

    @Test
    fun formatVolume_cubicMeters() {
        assertEquals("3.00 m³", MeasurementCalculator.formatVolume(3f, "m"))
    }
}

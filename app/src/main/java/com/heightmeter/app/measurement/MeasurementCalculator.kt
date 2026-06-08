package com.heightmeter.app.measurement

import com.heightmeter.app.measurement.model.Point3D
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * All mathematical calculations for AR measurements.
 * Uses real-world coordinates in meters.
 */
object MeasurementCalculator {

    private const val EPSILON = 0.0001f

    // ──────────────────────────────────────────────
    // Distance Calculations
    // ──────────────────────────────────────────────

    /**
     * Calculate 2D distance between two points (XZ plane - ground plane).
     */
    fun distance2D(a: Point3D, b: Point3D): Float {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * Calculate 3D distance between two points.
     */
    fun distance3D(a: Point3D, b: Point3D): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculate squared 3D distance (avoids sqrt for performance).
     */
    fun distance3DSquared(a: Point3D, b: Point3D): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return dx * dx + dy * dy + dz * dz
    }

    // ──────────────────────────────────────────────
    // Angle Calculations
    // ──────────────────────────────────────────────

    /**
     * Calculate the angle at vertex B between lines AB and BC.
     * Returns angle in degrees.
     */
    fun angleBetween(a: Point3D, b: Point3D, c: Point3D): Float {
        val ba = Point3D(x = a.x - b.x, y = a.y - b.y, z = a.z - b.z)
        val bc = Point3D(x = c.x - b.x, y = c.y - b.y, z = c.z - b.z)

        val dot = ba.x * bc.x + ba.y * bc.y + ba.z * bc.z
        val magBA = sqrt(ba.x * ba.x + ba.y * ba.y + ba.z * ba.z)
        val magBC = sqrt(bc.x * bc.x + bc.y * bc.y + bc.z * bc.z)

        if (magBA < EPSILON || magBC < EPSILON) return 0f

        val cosAngle = (dot / (magBA * magBC)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }

    // ──────────────────────────────────────────────
    // Area Calculations
    // ──────────────────────────────────────────────

    /**
     * Calculate area of a triangle defined by 3 points using cross product.
     */
    fun triangleArea(a: Point3D, b: Point3D, c: Point3D): Float {
        val ab = Point3D(x = b.x - a.x, y = b.y - a.y, z = b.z - a.z)
        val ac = Point3D(x = c.x - a.x, y = c.y - a.y, z = c.z - a.z)

        // Cross product magnitude / 2
        val cx = ab.y * ac.z - ab.z * ac.y
        val cy = ab.z * ac.x - ab.x * ac.z
        val cz = ab.x * ac.y - ab.y * ac.x

        return sqrt(cx * cx + cy * cy + cz * cz) * 0.5f
    }

    /**
     * Calculate area of a planar polygon in 3D using Newell's method.
     * Points must be in order (counter-clockwise or clockwise).
     *
     * Newell's method computes the polygon normal whose magnitude equals
     * twice the area of the polygon:
     *   Nx = Σ (y_i - y_{i+1}) * (z_i + z_{i+1})
     *   Ny = Σ (z_i - z_{i+1}) * (x_i + x_{i+1})
     *   Nz = Σ (x_i - x_{i+1}) * (y_i + y_{i+1})
     *   Area = 0.5 * |N|
     */
    fun polygonArea(points: List<Point3D>): Float {
        if (points.size < 3) return 0f

        val n = points.size
        var nx = 0f
        var ny = 0f
        var nz = 0f

        for (i in 0 until n) {
            val j = (i + 1) % n
            nx += (points[i].y - points[j].y) * (points[i].z + points[j].z)
            ny += (points[i].z - points[j].z) * (points[i].x + points[j].x)
            nz += (points[i].x - points[j].x) * (points[i].y + points[j].y)
        }

        return sqrt(nx * nx + ny * ny + nz * nz) * 0.5f
    }

    /**
     * Calculate area of a circle given radius.
     */
    fun circleArea(radius: Float): Float = PI.toFloat() * radius * radius

    /**
     * Calculate area of a rectangle given width and height.
     */
    fun rectangleArea(width: Float, height: Float): Float = width * height

    /**
     * Calculate area of a square given side length.
     */
    fun squareArea(side: Float): Float = side * side

    // ──────────────────────────────────────────────
    // Perimeter / Circumference
    // ──────────────────────────────────────────────

    /**
     * Calculate perimeter of a polygon (sum of edge lengths).
     */
    fun polygonPerimeter(points: List<Point3D>): Float {
        if (points.size < 2) return 0f
        var perimeter = 0f
        for (i in 0 until points.size) {
            val j = (i + 1) % points.size
            perimeter += distance3D(points[i], points[j])
        }
        return perimeter
    }

    /**
     * Calculate circumference of a circle.
     */
    fun circleCircumference(radius: Float): Float = 2f * PI.toFloat() * radius

    // ──────────────────────────────────────────────
    // Volume Calculations
    // ──────────────────────────────────────────────

    /**
     * Calculate volume of a cuboid.
     */
    fun cuboidVolume(width: Float, height: Float, depth: Float): Float =
        width * height * depth

    /**
     * Calculate volume of a cube.
     */
    fun cubeVolume(side: Float): Float = side * side * side

    /**
     * Calculate volume by extruding a base area by height.
     */
    fun extrudedVolume(baseArea: Float, height: Float): Float =
        baseArea * height

    /**
     * Calculate volume of a cylinder.
     */
    fun cylinderVolume(radius: Float, height: Float): Float =
        PI.toFloat() * radius * radius * height

    // ──────────────────────────────────────────────
    // Geometry Helpers
    // ──────────────────────────────────────────────

    /**
     * Calculate the midpoint between two points.
     */
    fun midpoint(a: Point3D, b: Point3D): Point3D = Point3D(
        x = (a.x + b.x) * 0.5f,
        y = (a.y + b.y) * 0.5f,
        z = (a.z + b.z) * 0.5f
    )

    /**
     * Calculate the centroid of a set of points.
     */
    fun centroid(points: List<Point3D>): Point3D {
        if (points.isEmpty()) return Point3D(x = 0f, y = 0f, z = 0f)
        val sumX = points.sumOf { it.x.toDouble() }.toFloat()
        val sumY = points.sumOf { it.y.toDouble() }.toFloat()
        val sumZ = points.sumOf { it.z.toDouble() }.toFloat()
        val n = points.size.toFloat()
        return Point3D(x = sumX / n, y = sumY / n, z = sumZ / n)
    }

    /**
     * Calculate the direction vector from a to b (normalized).
     */
    fun direction(a: Point3D, b: Point3D): Point3D {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val mag = sqrt(dx * dx + dy * dy + dz * dz)
        if (mag < EPSILON) return Point3D(x = 0f, y = 0f, z = 0f)
        return Point3D(x = dx / mag, y = dy / mag, z = dz / mag)
    }

    /**
     * Project a point onto the horizontal plane (y = 0).
     */
    fun projectToGround(point: Point3D): Point3D = point.copy(y = 0f)

    /**
     * Calculate the vertical height between two points (difference in Y).
     */
    fun verticalHeight(ground: Point3D, top: Point3D): Float =
        abs(top.y - ground.y)

    /**
     * Get the horizontal distance (ignoring Y component).
     */
    fun horizontalDistance(a: Point3D, b: Point3D): Float {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * Calculate a point at a given distance along a direction from a starting point.
     */
    fun pointAlongDirection(start: Point3D, direction: Point3D, distance: Float): Point3D =
        Point3D(
            x = start.x + direction.x * distance,
            y = start.y + direction.y * distance,
            z = start.z + direction.z * distance
        )

    /**
     * Find the closest point on a line segment to a given point.
     */
    fun closestPointOnSegment(p: Point3D, a: Point3D, b: Point3D): Point3D {
        val ab = direction(a, b)
        val ap = direction(a, p)
        val dot = ab.x * ap.x + ab.y * ap.y + ab.z * ap.z
        val t = dot.coerceIn(0f, distance3D(a, b))
        return pointAlongDirection(a, ab, t)
    }

    /**
     * Check if 3 points are collinear (within tolerance).
     */
    fun areCollinear(a: Point3D, b: Point3D, c: Point3D, tolerance: Float = 0.01f): Boolean {
        val area = triangleArea(a, b, c)
        return area < tolerance
    }

    /**
     * Calculate the normal vector of a plane defined by 3 points.
     */
    fun planeNormal(a: Point3D, b: Point3D, c: Point3D): Point3D {
        val ab = Point3D(x = b.x - a.x, y = b.y - a.y, z = b.z - a.z)
        val ac = Point3D(x = c.x - a.x, y = c.y - a.y, z = c.z - a.z)
        val nx = ab.y * ac.z - ab.z * ac.y
        val ny = ab.z * ac.x - ab.x * ac.z
        val nz = ab.x * ac.y - ab.y * ac.x
        val mag = sqrt(nx * nx + ny * ny + nz * nz)
        if (mag < EPSILON) return Point3D(x = 0f, y = 1f, z = 0f)
        return Point3D(x = nx / mag, y = ny / mag, z = nz / mag)
    }

    // ──────────────────────────────────────────────
    // Smooth Curve (Bezier) Interpolation
    // ──────────────────────────────────────────────

    /**
     * Generate smooth cubic Bezier curve through control points.
     * Returns interpolated points along the curve.
     */
    fun smoothCurve(points: List<Point3D>, segmentsPerPair: Int = 16): List<Point3D> {
        if (points.size < 2) return points
        if (points.size == 2) return points

        val result = mutableListOf<Point3D>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = if (i == 0) points[i] else points[i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i == points.size - 2) points[i + 1] else points[i + 2]

            for (t in 1..segmentsPerPair) {
                val tt = t.toFloat() / segmentsPerPair
                val point = cubicBezier(p0, p1, p2, p3, tt)
                result.add(point)
            }
        }

        return result.distinct()
    }

    /**
     * Cubic Bezier interpolation.
     */
    private fun cubicBezier(p0: Point3D, p1: Point3D, p2: Point3D, p3: Point3D, t: Float): Point3D {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        return Point3D(
            x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x,
            y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y,
            z = uuu * p0.z + 3 * uu * t * p1.z + 3 * u * tt * p2.z + ttt * p3.z
        )
    }

    // ──────────────────────────────────────────────
    // Unit Conversions
    // ──────────────────────────────────────────────

    /**
     * Format a measurement value with the appropriate unit suffix.
     */
    fun formatValue(value: Float, unit: String): String {
        return when (unit) {
            "m" -> String.format("%.2f m", value)
            "ft" -> String.format("%.2f ft", value)
            "cm" -> String.format("%.1f cm", value)
            "in" -> String.format("%.1f in", value)
            else -> String.format("%.2f", value)
        }
    }

    /**
     * Format area value.
     */
    fun formatArea(value: Float, unit: String): String {
        return when (unit) {
            "m" -> String.format("%.2f m²", value)
            "ft" -> String.format("%.2f ft²", value)
            else -> String.format("%.2f", value)
        }
    }

    /**
     * Format volume value.
     */
    fun formatVolume(value: Float, unit: String): String {
        return when (unit) {
            "m" -> String.format("%.2f m³", value)
            "ft" -> String.format("%.2f ft³", value)
            else -> String.format("%.2f", value)
        }
    }

    /**
     * Format angle in degrees.
     */
    fun formatAngle(degrees: Float): String =
        String.format("%.1f°", degrees)
}

package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Square tool - draws a perfect square.
 * Tap 2 points for one side, app auto-completes the square.
 */
class SquareTool : BaseTool() {

    override val type = MeasurementType.SQUARE
    override val displayName = "Square"
    override val iconResId = R.drawable.ic_square
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first corner"
        1 -> "Tap second corner (one side)"
        else -> "Square complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val sideLength = MeasurementCalculator.distance3D(points[0], points[1])
        val area = MeasurementCalculator.squareArea(sideLength)

        val corners = getSquareCorners(points)
        val centroid = MeasurementCalculator.centroid(corners)
        val sideLabel = "Side: ${MeasurementCalculator.formatValue(sideLength, unit)}"
        val areaLabel = "A: ${MeasurementCalculator.formatArea(area, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = sideLength,
            secondaryValue = area,
            points = corners,
            labels = listOf(
                LabelInfo(text = sideLabel, position = MeasurementCalculator.midpoint(points[0], points[1]), isPrimary = false),
                LabelInfo(text = areaLabel, position = centroid, isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        val corners = getSquareCorners(points)
        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until 4) {
            segments.add(Pair(corners[i], corners[(i + 1) % 4]))
        }
        return segments
    }

    /**
     * Returns the 4 corners of the complete square.
     * Uses a horizontal (XZ-plane) perpendicular so the square stays
     * on a level plane regardless of Y differences between points.
     */
    fun getSquareCorners(points: List<Point3D>): List<Point3D> {
        if (points.size < 2) return points
        val sideLength = MeasurementCalculator.distance3D(points[0], points[1])
        val dir = MeasurementCalculator.direction(points[0], points[1])

        // Project direction onto XZ plane for ground-level perpendicular
        val horizontalDir = Point3D(x = dir.x, y = 0f, z = dir.z)
        val perpDir = if (MeasurementCalculator.distance3DSquared(horizontalDir, Point3D.ZERO) > 0.0001f) {
            Point3D(x = -horizontalDir.z, y = 0f, z = horizontalDir.x)
        } else {
            // Fallback: direction is purely vertical, use X-axis as perpendicular
            Point3D(x = 1f, y = 0f, z = 0f)
        }

        return listOf(
            points[0],
            points[1],
            Point3D(
                x = points[1].x + perpDir.x * sideLength,
                y = points[1].y + perpDir.y * sideLength,
                z = points[1].z + perpDir.z * sideLength
            ),
            Point3D(
                x = points[0].x + perpDir.x * sideLength,
                y = points[0].y + perpDir.y * sideLength,
                z = points[0].z + perpDir.z * sideLength
            )
        )
    }
}

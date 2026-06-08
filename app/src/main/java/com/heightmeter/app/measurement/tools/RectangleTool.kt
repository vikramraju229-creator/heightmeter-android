package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Rectangle tool - draws a rectangle.
 * Tap 3 points: two corners for one side, then third for width direction.
 */
class RectangleTool : BaseTool() {

    override val type = MeasurementType.RECTANGLE
    override val displayName = "Rectangle"
    override val iconResId = R.drawable.ic_rectangle
    override val minPoints = 3
    override val maxPoints = 3

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first corner"
        1 -> "Tap second corner (one side)"
        2 -> "Tap width direction"
        else -> "Rectangle complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val width = MeasurementCalculator.distance3D(points[0], points[1])
        val dir = MeasurementCalculator.direction(points[0], points[1])

        // Calculate the height based on the third point's distance from the line
        val closestOnLine = MeasurementCalculator.closestPointOnSegment(points[2], points[0], points[1])
        val height = MeasurementCalculator.distance3D(points[2], closestOnLine)

        // Direction perpendicular to the base
        val perpDir = MeasurementCalculator.direction(closestOnLine, points[2])
        val normalizedPerp = if (MeasurementCalculator.distance3D(closestOnLine, points[2]) > 0.001f)
            perpDir
        else
            Point3D(x = -dir.z, y = 0f, z = dir.x)

        val corner3 = Point3D(
            x = points[1].x + normalizedPerp.x * height,
            y = points[1].y + normalizedPerp.y * height,
            z = points[1].z + normalizedPerp.z * height
        )
        val corner4 = Point3D(
            x = points[0].x + normalizedPerp.x * height,
            y = points[0].y + normalizedPerp.y * height,
            z = points[0].z + normalizedPerp.z * height
        )

        val allPoints = listOf(points[0], points[1], corner3, corner4)
        val centroid = MeasurementCalculator.centroid(allPoints)
        val area = MeasurementCalculator.rectangleArea(width, height)

        val widthLabel = "W: ${MeasurementCalculator.formatValue(width, unit)}"
        val heightLabel = "H: ${MeasurementCalculator.formatValue(height, unit)}"
        val areaLabel = "A: ${MeasurementCalculator.formatArea(area, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = width,
            secondaryValue = height,
            tertiaryValue = area,
            points = allPoints,
            labels = listOf(
                LabelInfo(text = widthLabel, position = MeasurementCalculator.midpoint(points[0], points[1]), isPrimary = false),
                LabelInfo(text = heightLabel, position = MeasurementCalculator.midpoint(points[1], corner3), isPrimary = false),
                LabelInfo(text = areaLabel, position = centroid, isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 3) return emptyList()
        return emptyList()  // Handled by calculating all corners
    }

    fun getRectangleCorners(points: List<Point3D>): List<Point3D> {
        if (points.size < 3) return points
        val dir = MeasurementCalculator.direction(points[0], points[1])
        val closestOnLine = MeasurementCalculator.closestPointOnSegment(points[2], points[0], points[1])
        val height = MeasurementCalculator.distance3D(points[2], closestOnLine)

        val perpDir = MeasurementCalculator.direction(closestOnLine, points[2])
        val normalizedPerp = if (MeasurementCalculator.distance3D(closestOnLine, points[2]) > 0.001f)
            perpDir
        else
            Point3D(x = -dir.z, y = 0f, z = dir.x)

        return listOf(
            points[0],
            points[1],
            Point3D(
                x = points[1].x + normalizedPerp.x * height,
                y = points[1].y + normalizedPerp.y * height,
                z = points[1].z + normalizedPerp.z * height
            ),
            Point3D(
                x = points[0].x + normalizedPerp.x * height,
                y = points[0].y + normalizedPerp.y * height,
                z = points[0].z + normalizedPerp.z * height
            )
        )
    }
}

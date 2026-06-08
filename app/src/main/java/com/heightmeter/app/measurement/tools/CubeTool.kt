package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Cube tool - tap 2 points to define one edge, auto-completes perfect cube.
 * Shows side length and volume.
 */
class CubeTool : BaseTool() {

    override val type = MeasurementType.CUBE
    override val displayName = "Cube"
    override val iconResId = R.drawable.ic_cube
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first corner"
        1 -> "Tap opposite corner"
        else -> "Cube complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val sideLength = MeasurementCalculator.distance3D(points[0], points[1])
        val volume = MeasurementCalculator.cubeVolume(sideLength)

        // Find the base corners (on the ground plane y = min y)
        val baseY = minOf(points[0].y, points[1].y)

        // Use the first point and the direction to the second to define the base square
        val dir = MeasurementCalculator.direction(points[0], points[1])
        val perpDir = Point3D(x = -dir.z, y = 0f, z = dir.x)

        val bottomCorners = listOf(
            Point3D(x = points[0].x, y = baseY, z = points[0].z),
            Point3D(x = points[0].x + dir.x * sideLength, y = baseY, z = points[0].z + dir.z * sideLength),
            Point3D(x = points[0].x + dir.x * sideLength + perpDir.x * sideLength, y = baseY, z = points[0].z + dir.z * sideLength + perpDir.z * sideLength),
            Point3D(x = points[0].x + perpDir.x * sideLength, y = baseY, z = points[0].z + perpDir.z * sideLength)
        )

        val topCorners = bottomCorners.map { it.copy(y = baseY + sideLength) }

        val allPoints = bottomCorners + topCorners
        val centroid = MeasurementCalculator.centroid(allPoints)

        val sideLabel = "Side: ${MeasurementCalculator.formatValue(sideLength, unit)}"
        val volumeLabel = "V: ${MeasurementCalculator.formatVolume(volume, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = sideLength,
            secondaryValue = volume,
            points = allPoints,
            labels = listOf(
                LabelInfo(text = sideLabel, position = MeasurementCalculator.midpoint(bottomCorners[0], bottomCorners[1]), isPrimary = false),
                LabelInfo(text = volumeLabel, position = centroid, isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()

        val sideLength = MeasurementCalculator.distance3D(points[0], points[1])
        val baseY = minOf(points[0].y, points[1].y)
        val dir = MeasurementCalculator.direction(points[0], points[1])
        val perpDir = Point3D(x = -dir.z, y = 0f, z = dir.x)

        val bot = listOf(
            Point3D(x = points[0].x, y = baseY, z = points[0].z),
            Point3D(x = points[0].x + dir.x * sideLength, y = baseY, z = points[0].z + dir.z * sideLength),
            Point3D(x = points[0].x + dir.x * sideLength + perpDir.x * sideLength, y = baseY, z = points[0].z + dir.z * sideLength + perpDir.z * sideLength),
            Point3D(x = points[0].x + perpDir.x * sideLength, y = baseY, z = points[0].z + perpDir.z * sideLength)
        )
        val top = bot.map { it.copy(y = baseY + sideLength) }

        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        // Bottom edges
        for (i in 0 until 4) segments.add(Pair(bot[i], bot[(i + 1) % 4]))
        // Top edges
        for (i in 0 until 4) segments.add(Pair(top[i], top[(i + 1) % 4]))
        // Vertical edges
        for (i in 0 until 4) segments.add(Pair(bot[i], top[i]))

        return segments
    }
}

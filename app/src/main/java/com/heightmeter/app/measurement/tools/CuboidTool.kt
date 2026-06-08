package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Cuboid tool - tap 2 opposite corners of base rectangle, then height point.
 * Shows full 3D cuboid with all dimensions and volume.
 */
class CuboidTool : BaseTool() {

    override val type = MeasurementType.CUBOID
    override val displayName = "Cuboid"
    override val iconResId = R.drawable.ic_cuboid
    override val minPoints = 3
    override val maxPoints = 3

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first base corner"
        1 -> "Tap opposite base corner"
        2 -> "Tap height point"
        else -> "Cuboid complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val corner1 = points[0]
        val corner2 = points[1]
        val heightPoint = points[2]

        // Compute width (horizontal) and depth (perpendicular in XZ plane).
        // The tool assumes the two base corners are opposite corners of an
        // axis-aligned rectangle on the XZ plane. For rotated rectangles,
        // use RectangleTool instead.
        val dx = corner2.x - corner1.x
        val dz = corner2.z - corner1.z
        val width = kotlin.math.abs(dx)
        val depth = kotlin.math.abs(dz)
        val height = MeasurementCalculator.verticalHeight(
            Point3D(x = corner1.x, y = minOf(corner1.y, corner2.y), z = corner1.z),
            heightPoint
        )

        val volume = MeasurementCalculator.cuboidVolume(width, height, depth)

        // Generate all 8 corners of the cuboid
        val baseY = minOf(corner1.y, corner2.y)
        val topY = baseY + height

        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minZ = minOf(corner1.z, corner2.z)
        val maxZ = maxOf(corner1.z, corner2.z)

        val bottomCorners = listOf(
            Point3D(x = minX, y = baseY, z = minZ),
            Point3D(x = maxX, y = baseY, z = minZ),
            Point3D(x = maxX, y = baseY, z = maxZ),
            Point3D(x = minX, y = baseY, z = maxZ)
        )
        val topCorners = bottomCorners.map { it.copy(y = topY) }

        val allPoints = bottomCorners + topCorners

        val volumeLabel = "V: ${MeasurementCalculator.formatVolume(volume, unit)}"
        val widthLabel = "W: ${MeasurementCalculator.formatValue(width, unit)}"
        val depthLabel = "D: ${MeasurementCalculator.formatValue(depth, unit)}"
        val heightLabel = "H: ${MeasurementCalculator.formatValue(height, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = volume,
            secondaryValue = width,
            tertiaryValue = height,
            points = allPoints,
            labels = listOf(
                LabelInfo(text = volumeLabel, position = MeasurementCalculator.centroid(allPoints), isPrimary = true),
                LabelInfo(text = widthLabel, position = MeasurementCalculator.midpoint(bottomCorners[0], bottomCorners[1]), isPrimary = false),
                LabelInfo(text = depthLabel, position = MeasurementCalculator.midpoint(bottomCorners[1], bottomCorners[2]), isPrimary = false),
                LabelInfo(text = heightLabel, position = MeasurementCalculator.midpoint(bottomCorners[0], topCorners[0]), isPrimary = false)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 3) return emptyList()

        val corner1 = points[0]
        val corner2 = points[1]
        val heightPoint = points[2]

        val baseY = minOf(corner1.y, corner2.y)
        val height = MeasurementCalculator.verticalHeight(
            Point3D(x = corner1.x, y = baseY, z = corner1.z),
            heightPoint
        )
        val topY = baseY + height

        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minZ = minOf(corner1.z, corner2.z)
        val maxZ = maxOf(corner1.z, corner2.z)

        val bot = listOf(
            Point3D(x = minX, y = baseY, z = minZ),
            Point3D(x = maxX, y = baseY, z = minZ),
            Point3D(x = maxX, y = baseY, z = maxZ),
            Point3D(x = minX, y = baseY, z = maxZ)
        )
        val top = bot.map { it.copy(y = topY) }

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

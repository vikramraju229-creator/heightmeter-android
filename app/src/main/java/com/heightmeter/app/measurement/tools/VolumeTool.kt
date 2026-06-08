package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Volume tool - tap 4 base corners then height point.
 * Shows volume in cubic meters.
 */
class VolumeTool : BaseTool() {

    override val type = MeasurementType.VOLUME
    override val displayName = "Volume"
    override val iconResId = R.drawable.ic_volume
    override val minPoints = 5
    override val maxPoints = 5

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first base corner"
        1 -> "Tap second base corner"
        2 -> "Tap third base corner"
        3 -> "Tap fourth base corner"
        4 -> "Tap height point"
        else -> "Volume complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        // Base is first 4 points, height from base to last point
        val baseArea = MeasurementCalculator.polygonArea(points.take(4))
        val baseCentroid = MeasurementCalculator.centroid(points.take(4))
        val height = MeasurementCalculator.verticalHeight(baseCentroid, points[4])
        val volume = MeasurementCalculator.extrudedVolume(baseArea, height)

        val volumeLabel = "V: ${MeasurementCalculator.formatVolume(volume, unit)}"
        val areaLabel = "Base: ${MeasurementCalculator.formatArea(baseArea, unit)}"
        val heightLabel = "H: ${MeasurementCalculator.formatValue(height, unit)}"

        // Calculate top 4 corners
        val heightOffset = points[4].y - baseCentroid.y
        val topPoints = points.take(4).map { it.copy(y = it.y + heightOffset) }

        val allPoints = points.take(4) + topPoints + listOf(points[4])

        return MeasurementResult(
            type = type,
            primaryValue = volume,
            secondaryValue = baseArea,
            tertiaryValue = height,
            points = allPoints,
            labels = listOf(
                LabelInfo(text = volumeLabel, position = MeasurementCalculator.midpoint(baseCentroid, baseCentroid.copy(y = baseCentroid.y + height)), isPrimary = true),
                LabelInfo(text = areaLabel, position = baseCentroid, isPrimary = false),
                LabelInfo(text = heightLabel, position = MeasurementCalculator.midpoint(baseCentroid, points[4]), isPrimary = false)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 5) return emptyList()

        val baseCentroid = MeasurementCalculator.centroid(points.take(4))
        val heightOffset = points[4].y - baseCentroid.y
        val topPoints = points.take(4).map { it.copy(y = it.y + heightOffset) }

        val segments = mutableListOf<Pair<Point3D, Point3D>>()

        // Base edges
        for (i in 0 until 4) {
            segments.add(Pair(points[i], points[(i + 1) % 4]))
        }
        // Top edges
        for (i in 0 until 4) {
            segments.add(Pair(topPoints[i], topPoints[(i + 1) % 4]))
        }
        // Vertical edges
        for (i in 0 until 4) {
            segments.add(Pair(points[i], topPoints[i]))
        }
        // Height indicator
        segments.add(Pair(baseCentroid, points[4]))

        return segments
    }
}

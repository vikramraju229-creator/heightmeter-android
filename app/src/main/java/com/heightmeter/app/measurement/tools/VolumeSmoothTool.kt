package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Volume Smooth tool - tap multiple base points (3+) then height point.
 * Base uses smooth Bezier curve, then extrudes to show volume.
 */
class VolumeSmoothTool : BaseTool() {

    override val type = MeasurementType.VOLUME_SMOOTH
    override val displayName = "Volume Smooth"
    override val iconResId = R.drawable.ic_volume
    override val minPoints = 4  // 3 base + 1 height
    override val maxPoints = -1 // unlimited base points

    override val autoComplete: Boolean = false
    override val useSmoothCurves: Boolean = true

    override fun getInstructions(currentPointCount: Int): String = when {
        currentPointCount < 3 -> "Tap base points (need at least 3)"
        currentPointCount == 3 -> "Tap height point to finish"
        else -> "Measurement complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val basePoints = points.dropLast(1)
        val heightPoint = points.last()

        val baseCentroid = MeasurementCalculator.centroid(basePoints)
        val height = MeasurementCalculator.verticalHeight(baseCentroid, heightPoint)

        // Calculate smooth base area
        val baseArea = MeasurementCalculator.polygonArea(basePoints)
        val volume = MeasurementCalculator.extrudedVolume(baseArea, height)

        // Generate smooth curve for base
        val closedBase = basePoints + basePoints.first()
        val smoothBase = MeasurementCalculator.smoothCurve(closedBase)

        val volumeLabel = "V: ${MeasurementCalculator.formatVolume(volume, unit)}"
        val areaLabel = "Base: ${MeasurementCalculator.formatArea(baseArea, unit)}"

        val allPoints = smoothBase + listOf(heightPoint)

        return MeasurementResult(
            type = type,
            primaryValue = volume,
            secondaryValue = baseArea,
            tertiaryValue = height,
            points = allPoints,
            labels = listOf(
                LabelInfo(text = volumeLabel, position = MeasurementCalculator.midpoint(baseCentroid, baseCentroid.copy(y = baseCentroid.y + height)), isPrimary = true),
                LabelInfo(text = areaLabel, position = baseCentroid, isPrimary = false)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 4) return emptyList()

        val basePoints = points.dropLast(1)
        val heightPoint = points.last()
        val baseCentroid = MeasurementCalculator.centroid(basePoints)
        val heightOffset = heightPoint.y - baseCentroid.y

        // Generate smooth curve points for base and top
        val closedBase = basePoints + basePoints.first()
        val smoothBase = MeasurementCalculator.smoothCurve(closedBase)
        val smoothTop = smoothBase.map { it.copy(y = it.y + heightOffset) }

        val segments = mutableListOf<Pair<Point3D, Point3D>>()

        // Base curve edges
        for (i in 0 until smoothBase.size - 1) {
            segments.add(Pair(smoothBase[i], smoothBase[i + 1]))
        }
        // Top curve edges
        for (i in 0 until smoothTop.size - 1) {
            segments.add(Pair(smoothTop[i], smoothTop[i + 1]))
        }
        // Some vertical lines (simplified - draw from every Nth base point)
        val step = maxOf(1, smoothBase.size / 8)
        for (i in smoothBase.indices step step) {
            segments.add(Pair(smoothBase[i], smoothTop[i]))
        }
        // Height indicator
        segments.add(Pair(baseCentroid, heightPoint))

        return segments
    }
}

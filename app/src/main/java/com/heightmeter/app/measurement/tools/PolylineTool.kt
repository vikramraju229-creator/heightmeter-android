package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Polyline tool - draws connected line segments through multiple points.
 * Shows total length of the polyline.
 */
class PolylineTool : BaseTool() {

    override val type = MeasurementType.POLYLINE
    override val displayName = "Polyline"
    override val iconResId = R.drawable.ic_polyline
    override val minPoints = 2
    override val maxPoints = -1  // unlimited

    override val autoComplete: Boolean = false

    override fun getInstructions(currentPointCount: Int): String = when {
        currentPointCount < 2 -> "Tap to add points (need at least 2)"
        else -> "Tap to add more points. Press Save to finish."
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val totalDistance = MeasurementCalculator.polygonPerimeter(points)
        val label = "Total: ${MeasurementCalculator.formatValue(totalDistance, unit)}"
        val lastMidpoint = if (points.size >= 2)
            MeasurementCalculator.midpoint(points.last(), points[points.size - 2])
        else Point3D(x = 0f, y = 0f, z = 0f)

        return MeasurementResult(
            type = type,
            primaryValue = totalDistance,
            points = points,
            labels = listOf(
                LabelInfo(text = label, position = lastMidpoint, isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until points.size - 1) {
            segments.add(Pair(points[i], points[i + 1]))
        }
        return segments
    }
}

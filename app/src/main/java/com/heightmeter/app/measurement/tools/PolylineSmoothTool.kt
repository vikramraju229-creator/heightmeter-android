package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Polyline Smooth tool - draws smooth Bezier curves through control points.
 * Shows total arc length of the curve.
 */
class PolylineSmoothTool : BaseTool() {

    override val type = MeasurementType.POLYLINE_SMOOTH
    override val displayName = "Polyline Smooth"
    override val iconResId = R.drawable.ic_polyline
    override val minPoints = 2
    override val maxPoints = -1  // unlimited

    override val autoComplete: Boolean = false
    override val useSmoothCurves: Boolean = true

    override fun getInstructions(currentPointCount: Int): String = when {
        currentPointCount < 2 -> "Tap to add points (need at least 2)"
        else -> "Tap to add more points. Press Save to finish."
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val curvePoints = MeasurementCalculator.smoothCurve(points)
        var totalLength = 0f
        for (i in 0 until curvePoints.size - 1) {
            totalLength += MeasurementCalculator.distance3D(curvePoints[i], curvePoints[i + 1])
        }
        val label = "Total: ${MeasurementCalculator.formatValue(totalLength, unit)}"
        val lastMidpoint = if (points.size >= 2)
            MeasurementCalculator.midpoint(points.last(), points[points.size - 2])
        else Point3D(x = 0f, y = 0f, z = 0f)

        return MeasurementResult(
            type = type,
            primaryValue = totalLength,
            points = curvePoints,
            labels = listOf(
                LabelInfo(text = label, position = lastMidpoint, isPrimary = true)
            )
        )
    }

    override fun getCurvePoints(points: List<Point3D>): List<Point3D> {
        return MeasurementCalculator.smoothCurve(points)
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        val curvePoints = getCurvePoints(points)
        if (curvePoints.size < 2) return emptyList()
        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until curvePoints.size - 1) {
            segments.add(Pair(curvePoints[i], curvePoints[i + 1]))
        }
        return segments
    }
}

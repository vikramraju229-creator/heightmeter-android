package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Poly Smooth tool - closed smooth polygon using Bezier curves.
 * Shows perimeter and area.
 */
class PolySmoothTool : BaseTool() {

    override val type = MeasurementType.POLY_SMOOTH
    override val displayName = "Poly Smooth"
    override val iconResId = R.drawable.ic_polygon
    override val minPoints = 3
    override val maxPoints = -1  // unlimited

    override val autoComplete: Boolean = false
    override val useSmoothCurves: Boolean = true

    override fun getInstructions(currentPointCount: Int): String = when {
        currentPointCount < 3 -> "Tap to add points (need at least 3)"
        else -> "Tap to add more points. Press Save to finish."
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        // Add the first point at the end to close the smooth curve
        val closedPoints = points + points.first()
        val curvePoints = MeasurementCalculator.smoothCurve(closedPoints)

        // Calculate perimeter from curve points
        var perimeter = 0f
        for (i in 0 until curvePoints.size - 1) {
            perimeter += MeasurementCalculator.distance3D(curvePoints[i], curvePoints[i + 1])
        }

        // Approximate area from original polygon points
        val area = MeasurementCalculator.polygonArea(points)
        val centroid = MeasurementCalculator.centroid(points)

        val areaLabel = "A: ${MeasurementCalculator.formatArea(area, unit)}"
        val perimeterLabel = "P: ${MeasurementCalculator.formatValue(perimeter, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = perimeter,
            secondaryValue = area,
            points = curvePoints,
            labels = listOf(
                LabelInfo(text = areaLabel, position = centroid, isPrimary = true),
                LabelInfo(text = perimeterLabel, position = centroid.copy(y = centroid.y + 0.1f), isPrimary = false)
            )
        )
    }

    override fun getCurvePoints(points: List<Point3D>): List<Point3D> {
        val closedPoints = points + points.first()
        return MeasurementCalculator.smoothCurve(closedPoints)
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        val curvePoints = getCurvePoints(points)
        if (curvePoints.size < 2) return emptyList()
        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until curvePoints.size - 1) {
            segments.add(Pair(curvePoints[i], curvePoints[i + 1]))
        }
        // Close
        segments.add(Pair(curvePoints.last(), curvePoints.first()))
        return segments
    }
}

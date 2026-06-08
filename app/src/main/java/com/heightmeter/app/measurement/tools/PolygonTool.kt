package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Polygon tool - draws closed polygon through 3+ points.
 * Shows perimeter and area.
 */
class PolygonTool : BaseTool() {

    override val type = MeasurementType.POLYGON
    override val displayName = "Polygon"
    override val iconResId = R.drawable.ic_polygon
    override val minPoints = 3
    override val maxPoints = -1  // unlimited

    override val autoComplete: Boolean = false

    override fun getInstructions(currentPointCount: Int): String = when {
        currentPointCount < 3 -> "Tap to add points (need at least 3)"
        else -> "Tap to add more points. Press Save to finish."
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val perimeter = MeasurementCalculator.polygonPerimeter(points)
        val area = MeasurementCalculator.polygonArea(points)
        val centroid = MeasurementCalculator.centroid(points)

        val perimeterLabel = "P: ${MeasurementCalculator.formatValue(perimeter, unit)}"
        val areaLabel = "A: ${MeasurementCalculator.formatArea(area, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = perimeter,
            secondaryValue = area,
            points = points,
            labels = listOf(
                LabelInfo(text = areaLabel, position = centroid, isPrimary = true),
                LabelInfo(text = perimeterLabel, position = centroid.copy(y = centroid.y + 0.1f), isPrimary = false)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until points.size - 1) {
            segments.add(Pair(points[i], points[i + 1]))
        }
        // Close the polygon
        if (points.size >= 3) {
            segments.add(Pair(points.last(), points.first()))
        }
        return segments
    }
}

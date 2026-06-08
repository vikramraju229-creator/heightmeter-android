package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Line tool - measures straight line distance between 2 points.
 */
class LineTool : BaseTool() {

    override val type = MeasurementType.LINE
    override val displayName = "Line"
    override val iconResId = R.drawable.ic_line
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap to place first point"
        1 -> "Tap to place second point"
        else -> "Line complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val distance = MeasurementCalculator.distance3D(points[0], points[1])
        val label = MeasurementCalculator.formatValue(distance, unit)
        val midpoint = MeasurementCalculator.midpoint(points[0], points[1])

        return MeasurementResult(
            type = type,
            primaryValue = distance,
            points = points,
            labels = listOf(
                LabelInfo(text = label, position = midpoint, isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        return listOf(Pair(points[0], points[1]))
    }
}

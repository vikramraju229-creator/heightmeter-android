package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Distance tool - measures 3D distance between any 2 points in space.
 * Unlike Line tool, this always shows 3D distance regardless of plane.
 */
class DistanceTool : BaseTool() {

    override val type = MeasurementType.DISTANCE
    override val displayName = "Distance"
    override val iconResId = R.drawable.ic_distance
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first point"
        1 -> "Tap second point"
        else -> "Distance complete"
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

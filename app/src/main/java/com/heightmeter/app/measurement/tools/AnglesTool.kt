package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Angles tool - measures angle at a vertex between 2 points.
 * Tap: point A, vertex B, point C.
 * Shows angle at B.
 */
class AnglesTool : BaseTool() {

    override val type = MeasurementType.ANGLES
    override val displayName = "Angles"
    override val iconResId = R.drawable.ic_angles
    override val minPoints = 3
    override val maxPoints = 3

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap first endpoint (A)"
        1 -> "Tap vertex point (B)"
        2 -> "Tap second endpoint (C)"
        else -> "Angle complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val angle = MeasurementCalculator.angleBetween(points[0], points[1], points[2])
        val label = MeasurementCalculator.formatAngle(angle)

        val labels = listOf(
            LabelInfo(
                text = label,
                position = points[1],  // vertex
                isPrimary = true
            )
        )

        return MeasurementResult(
            type = type,
            primaryValue = angle,
            points = points,
            labels = labels
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 3) return emptyList()
        return listOf(
            Pair(points[0], points[1]),  // A to vertex
            Pair(points[1], points[2])   // vertex to C
        )
    }
}

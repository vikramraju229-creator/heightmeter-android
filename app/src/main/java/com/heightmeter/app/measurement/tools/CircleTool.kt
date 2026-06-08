package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Circle tool - tap center then edge point to define a circle.
 * Shows radius, diameter, circumference, and area.
 */
class CircleTool : BaseTool() {

    override val type = MeasurementType.CIRCLE
    override val displayName = "Circle"
    override val iconResId = R.drawable.ic_circle
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap center point"
        1 -> "Tap edge point"
        else -> "Circle complete"
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val radius = MeasurementCalculator.distance3D(points[0], points[1])
        val diameter = radius * 2f
        val area = MeasurementCalculator.circleArea(radius)

        val radiusLabel = "R: ${MeasurementCalculator.formatValue(radius, unit)}"
        val areaLabel = "A: ${MeasurementCalculator.formatArea(area, unit)}"

        return MeasurementResult(
            type = type,
            primaryValue = radius,
            secondaryValue = diameter,
            tertiaryValue = area,
            points = points,
            labels = listOf(
                LabelInfo(text = radiusLabel, position = MeasurementCalculator.midpoint(points[0], points[1]), isPrimary = false),
                LabelInfo(text = areaLabel, position = points[0].copy(y = points[0].y + 0.1f), isPrimary = true)
            )
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        // Draw radius line from center to edge
        return listOf(Pair(points[0], points[1]))
    }

    /**
     * Generate points along the circle circumference for rendering.
     */
    fun getCirclePoints(center: Point3D, radius: Float, segments: Int = 36): List<Point3D> {
        val points = mutableListOf<Point3D>()
        for (i in 0 until segments) {
            val angle = (2f * Math.PI.toFloat() * i.toFloat()) / segments.toFloat()
            val x = center.x + radius * kotlin.math.cos(angle.toDouble()).toFloat()
            val z = center.z + radius * kotlin.math.sin(angle.toDouble()).toFloat()
            points.add(Point3D(x = x, y = center.y, z = z))
        }
        return points
    }
}

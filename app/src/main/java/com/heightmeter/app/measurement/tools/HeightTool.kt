package com.heightmeter.app.measurement.tools

import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.R

/**
 * Height tool - measures vertical height.
 * Tap two points (ground and top, in any order) to display height.
 * Automatically detects which point is the ground (lower Y) and which is the top (higher Y).
 */
class HeightTool : BaseTool() {

    override val type = MeasurementType.HEIGHT
    override val displayName = "Height"
    override val iconResId = R.drawable.ic_height
    override val minPoints = 2
    override val maxPoints = 2

    override fun getInstructions(currentPointCount: Int): String = when (currentPointCount) {
        0 -> "Tap ground point"
        1 -> "Tap top point"
        else -> "Height complete"
    }

    /**
     * Determine ground (lower Y) and top (higher Y) from two points.
     * In ARCore, Y increases upward, so the ground has the smaller Y value.
     */
    private fun resolveGroundAndTop(points: List<Point3D>): Pair<Point3D, Point3D> {
        val a = points[0]
        val b = points[1]
        return if (a.y <= b.y) Pair(a, b) else Pair(b, a)
    }

    override fun calculate(points: List<Point3D>, unit: String): MeasurementResult {
        val (ground, top) = resolveGroundAndTop(points)
        val height = MeasurementCalculator.verticalHeight(ground, top)
        val label = MeasurementCalculator.formatValue(height, unit)

        // Project the top point straight down to ground Y for the vertical line
        val groundProjection = top.copy(y = ground.y)

        val labels = listOf(
            LabelInfo(
                text = label,
                position = MeasurementCalculator.midpoint(ground, top),
                isPrimary = true
            )
        )

        return MeasurementResult(
            type = type,
            primaryValue = height,
            points = listOf(ground, groundProjection, top),
            labels = labels
        )
    }

    override fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> {
        if (points.size < 2) return emptyList()
        val (ground, top) = resolveGroundAndTop(points)
        val groundProjection = top.copy(y = ground.y)
        return listOf(
            Pair(ground, groundProjection),  // horizontal base on ground plane
            Pair(groundProjection, top)      // vertical height
        )
    }
}

package com.heightmeter.app.measurement

import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D

/**
 * Abstract base class for all AR measurement tools.
 * Each tool defines its own point-placement logic, validation, and result calculation.
 */
abstract class BaseTool {

    /** The type of measurement this tool performs. */
    abstract val type: MeasurementType

    /** Display name shown in the UI. */
    abstract val displayName: String

    /** Icon resource ID for the tool button. */
    abstract val iconResId: Int

    /** The minimum number of points needed to compute a result. */
    abstract val minPoints: Int

    /** The maximum number of points this tool accepts (or -1 for unlimited). */
    abstract val maxPoints: Int

    /** Description of the current step shown to the user. */
    abstract fun getInstructions(currentPointCount: Int): String

    /**
     * Validate current points. Return null if valid, or an error message.
     */
    open fun validate(points: List<Point3D>): String? = null

    /**
     * Calculate measurement result from placed points.
     * Called when enough points are placed.
     */
    abstract fun calculate(points: List<Point3D>, unit: String): MeasurementResult

    /**
     * Whether the tool automatically finishes when maxPoints are reached.
     */
    open val autoComplete: Boolean = true

    /**
     * Get the 3D positions that define line segments to draw.
     * Each pair of consecutive points forms a line segment.
     * Return empty list for no lines.
     */
    open fun getLineSegments(points: List<Point3D>): List<Pair<Point3D, Point3D>> = emptyList()

    /**
     * Get the positions where point markers should be displayed.
     */
    open fun getMarkerPositions(points: List<Point3D>): List<Point3D> = points

    /**
     * Whether this tool uses smooth (Bezier) curves for rendering.
     */
    open val useSmoothCurves: Boolean = false

    /**
     * Get smooth curve control points if applicable.
     */
    open fun getCurvePoints(points: List<Point3D>): List<Point3D> = points

    /**
     * Called when the user places a new point.
     * Can be overridden to modify the point (e.g., snap to surface).
     */
    open fun onPointAdded(points: MutableList<Point3D>, newPoint: Point3D): Point3D = newPoint
}

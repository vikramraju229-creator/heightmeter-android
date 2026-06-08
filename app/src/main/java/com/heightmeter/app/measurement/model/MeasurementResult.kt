package com.heightmeter.app.measurement.model

/**
 * Holds the result of a measurement calculation for display.
 */
data class MeasurementResult(
    val type: MeasurementType,
    val primaryValue: Float,
    val secondaryValue: Float? = null,
    val tertiaryValue: Float? = null,
    val points: List<Point3D> = emptyList(),
    val labels: List<LabelInfo> = emptyList()
)

/**
 * Types of measurements supported by the app.
 */
enum class MeasurementType(val displayName: String) {
    LINE("Line"),
    HEIGHT("Height"),
    ANGLES("Angles"),
    DISTANCE("Distance"),
    POLYLINE("Polyline"),
    POLYLINE_SMOOTH("Polyline Smooth"),
    POLYGON("Polygon"),
    POLY_SMOOTH("Poly Smooth"),
    SQUARE("Square"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    VOLUME("Volume"),
    VOLUME_SMOOTH("Volume Smooth"),
    CUBOID("Cuboid"),
    CUBE("Cube")
}

/**
 * Information about a label to be displayed in AR space.
 */
data class LabelInfo(
    val text: String,
    val position: Point3D,
    val isPrimary: Boolean = true
)

package com.heightmeter.app.measurement.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heightmeter.app.measurement.model.MeasurementType

/**
 * Room entity for storing saved measurements.
 */
@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val typeName: String,
    val primaryValue: Double,
    val secondaryValue: Double? = null,
    val tertiaryValue: Double? = null,
    val pointsJson: String,        // JSON array of 3D points
    val screenshotPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val unit: String = "m",
    val label: String = ""
) {
    /** Safely resolve [MeasurementType] from [typeName], falling back to [MeasurementType.LINE]. */
    val measurementType: MeasurementType
        get() = safeMeasurementType(typeName)

    companion object {
        private val TYPE_NAME_CACHE: Map<String, MeasurementType> =
            MeasurementType.entries.associateBy { it.name }

        /** Resolve type name safely. O(1) lookup; falls back to LINE. */
        private fun safeMeasurementType(name: String): MeasurementType =
            TYPE_NAME_CACHE[name] ?: MeasurementType.LINE
    }
}

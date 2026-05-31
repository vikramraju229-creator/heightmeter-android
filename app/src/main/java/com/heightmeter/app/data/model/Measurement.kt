package com.heightmeter.app.data.model

/**
 * Domain model for a height measurement.
 */
data class Measurement(
    val id: Long = 0,
    val personName: String,
    val heightCm: Float,
    val accuracyLabel: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Returns height in feet and inches (e.g. "5 ft 10 in").
     */
    fun toFeetInches(): String {
        val totalInches = (heightCm / 2.54).toInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return "${feet} ft ${inches} in"
    }

    /**
     * Returns a formatted date string.
     */
    fun formattedDate(): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy  HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

package com.heightmeter.app.data.model

/**
 * Holds the result of a height calculation.
 */
data class HeightResult(
    val heightCm: Float,
    val confidenceHead: Float,
    val confidenceFeet: Float,
    val accuracyLabel: String
) {
    companion object {
        /**
         * Determines accuracy label based on average landmark confidence.
         */
        fun determineAccuracy(avgConfidence: Float): String = when {
            avgConfidence >= 0.9f -> "Excellent"
            avgConfidence >= 0.8f -> "Good"
            else -> "Fair"
        }
    }
}

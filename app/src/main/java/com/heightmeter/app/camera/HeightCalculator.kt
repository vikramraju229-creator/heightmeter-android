package com.heightmeter.app.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SizeF
import com.heightmeter.app.data.model.HeightResult
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.tan

/**
 * Calculates a person's height from pose landmarks, camera FOV, and tilt.
 *
 * Formula (from spec):
 *   pixel_distance      = nose_y - ankle_midpoint_y
 *   screen_ratio        = pixel_distance / screen_height_pixels
 *   fov_angle           = from CameraCharacteristics
 *   tilt_radians        = from accelerometer pitch
 *   estimated_height_mm = screen_ratio * 2 * PHONE_HEIGHT_MM * tan(fov/2) / cos(tilt)
 *   final_height_cm     = estimated_height_mm / 10 * CORRECTION_FACTOR
 */
class HeightCalculator(private val cameraManager: CameraManager) {

    companion object {
        /** Assumed phone distance from floor in mm (1.2 m). */
        private const val PHONE_HEIGHT_MM = 1200f

        /** Correction factor because nose is not the top of the head. */
        private const val CORRECTION_FACTOR = 1.07f

        /** Minimum confidence threshold for landmark detection. */
        const val MIN_CONFIDENCE = 0.7f
    }

    /**
     * Calculates height from pose analysis result and tilt correction.
     *
     * @param result the pose analysis result with valid landmarks
     * @param cameraId the camera ID used to read FOV characteristics
     * @param tiltCorrectionFactor cosine of pitch angle (1.0 = no tilt)
     * @return [HeightResult] or null if landmarks are insufficient
     */
    fun calculateHeight(
        result: PoseAnalysisResult,
        cameraId: String,
        tiltCorrectionFactor: Float
    ): HeightResult? {
        val nose = result.nose ?: return null
        val leftAnkle = result.leftAnkle
        val rightAnkle = result.rightAnkle

        // Need at least one ankle detected
        val ankleY = when {
            leftAnkle != null && rightAnkle != null ->
                (leftAnkle.position.y + rightAnkle.position.y) / 2f
            leftAnkle != null -> leftAnkle.position.y
            rightAnkle != null -> rightAnkle.position.y
            else -> return null
        }

        // Validate confidence thresholds
        if (result.noseConfidence < MIN_CONFIDENCE) return null
        if (result.ankleConfidence < MIN_CONFIDENCE) return null

        val noseY = nose.position.y

        // pixel_distance (absolute value, since nose is above ankles in the image)
        val pixelDistance = abs(noseY - ankleY)
        if (pixelDistance <= 0f) return null

        // screen_ratio
        val screenHeightPx = result.imageHeight.toFloat()
        val screenRatio = pixelDistance / screenHeightPx

        // Get FOV angle
        val fovAngle = getFovAngle(cameraId)
        if (fovAngle <= 0f) return null

        // Height calculation
        val fovRad = Math.toRadians(fovAngle.toDouble()).toFloat()
        val estimatedHeightMm = screenRatio * 2f * PHONE_HEIGHT_MM * tan(fovRad / 2f) / tiltCorrectionFactor
        val heightCm = (estimatedHeightMm / 10f) * CORRECTION_FACTOR

        // Clamp to reasonable range (50 cm - 250 cm)
        val clampedHeight = heightCm.coerceIn(50f, 250f)

        val avgConfidence = (result.noseConfidence + result.ankleConfidence) / 2f
        val accuracyLabel = HeightResult.determineAccuracy(avgConfidence)

        return HeightResult(
            heightCm = clampedHeight,
            confidenceHead = result.noseConfidence,
            confidenceFeet = result.ankleConfidence,
            accuracyLabel = accuracyLabel
        )
    }

    /**
     * Reads the horizontal FOV from camera characteristics.
     * Falls back to a reasonable default if unavailable.
     */
    private fun getFovAngle(cameraId: String): Float {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            // Use the sensor physical size and focal length to compute FOV
            val physicalSize: SizeF? =
                characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val focalLengths: FloatArray? =
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

            if (physicalSize != null && focalLengths != null && focalLengths.isNotEmpty()) {
                val focalLength = focalLengths[0]
                // Horizontal FOV = 2 * atan(sensor_width / (2 * focal_length))
                val fovRad = 2.0 * Math.atan((physicalSize.width / 2.0) / focalLength)
                Math.toDegrees(fovRad).toFloat()
            } else {
                // Default FOV (~63 degrees for typical phone rear camera)
                63f
            }
        } catch (e: Exception) {
            63f
        }
    }
}

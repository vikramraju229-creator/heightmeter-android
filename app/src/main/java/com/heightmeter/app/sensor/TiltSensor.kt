package com.heightmeter.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.atan2

/**
 * Monitors the accelerometer to compute the phone's tilt (pitch angle).
 * Pitch is the rotation around the X-axis in radians.
 * 0 = phone held vertically (upright), positive = tilted backward.
 */
class TiltSensor(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Current pitch angle in radians. 0 when phone is upright (portrait). */
    @Volatile
    var pitchRadians: Float = 0f
        private set

    /** Whether the sensor is currently registered. */
    private var isRegistered = false

    /**
     * Starts listening for accelerometer updates.
     * Call from onResume().
     */
    fun start() {
        if (!isRegistered && accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            isRegistered = true
        }
    }

    /**
     * Stops listening for accelerometer updates.
     * Call from onPause().
     */
    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val (ax, ay, az) = event.values
            // Pitch = rotation around X-axis
            // When phone is upright in portrait: pitch ≈ 0
            // When top tilts back: pitch > 0
            pitchRadians = atan2(-ax, az)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Returns the cosine of the pitch angle for height correction.
     * Used to divide the estimated height: height_corrected = height / cos(pitch).
     */
    fun getTiltCorrectionFactor(): Float {
        return kotlin.math.cos(pitchRadians).coerceAtLeast(0.5f) // prevent extreme values
    }
}

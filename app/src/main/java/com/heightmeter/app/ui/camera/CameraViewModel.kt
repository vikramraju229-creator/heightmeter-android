package com.heightmeter.app.ui.camera

import android.app.Application
import android.hardware.camera2.CameraManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.heightmeter.app.camera.HeightCalculator
import com.heightmeter.app.camera.PoseAnalysisResult
import com.heightmeter.app.data.db.AppDatabase
import com.heightmeter.app.data.db.MeasurementEntity
import com.heightmeter.app.data.model.HeightResult
import com.heightmeter.app.sensor.TiltSensor
import kotlinx.coroutines.launch

/**
 * ViewModel for the Camera / Pose Detection screen.
 * Manages pose state, height calculation results, and database operations.
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.measurementDao()

    val tiltSensor = TiltSensor(application)

    private val cameraManager =
        application.getSystemService(Application.CAMERA_SERVICE) as CameraManager

    val heightCalculator = HeightCalculator(cameraManager)

    // --- LiveData ---

    private val _poseResult = MutableLiveData<PoseAnalysisResult?>()
    val poseResult: LiveData<PoseAnalysisResult?> = _poseResult

    private val _headConfidence = MutableLiveData<Float>(0f)
    val headConfidence: LiveData<Float> = _headConfidence

    private val _ankleConfidence = MutableLiveData<Float>(0f)
    val ankleConfidence: LiveData<Float> = _ankleConfidence

    private val _headDetected = MutableLiveData(false)
    val headDetected: LiveData<Boolean> = _headDetected

    private val _feetDetected = MutableLiveData(false)
    val feetDetected: LiveData<Boolean> = _feetDetected

    private val _captureEnabled = MutableLiveData(false)
    val captureEnabled: LiveData<Boolean> = _captureEnabled

    private val _calculatedResult = MutableLiveData<HeightResult?>()
    val calculatedResult: LiveData<HeightResult?> = _calculatedResult

    private val _showGuide = MutableLiveData(true)
    val showGuide: LiveData<Boolean> = _showGuide

    /** The currently active camera ID for FOV reading. */
    var currentCameraId: String = "0"

    /**
     * Processes a new pose analysis result from the ML Kit analyzer.
     * Updates all LiveData observables.
     */
    fun onPoseAnalyzed(result: PoseAnalysisResult) {
        _poseResult.postValue(result)

        val headOk = result.noseConfidence >= HeightCalculator.MIN_CONFIDENCE
        val feetOk = result.ankleConfidence >= HeightCalculator.MIN_CONFIDENCE

        _headConfidence.postValue(result.noseConfidence)
        _ankleConfidence.postValue(result.ankleConfidence)
        _headDetected.postValue(headOk)
        _feetDetected.postValue(feetOk)
        _captureEnabled.postValue(headOk && feetOk)
        _showGuide.postValue(!(headOk && feetOk))
    }

    /**
     * Calculates height from the latest pose data and tilt sensor.
     * Posts the result to [calculatedResult].
     */
    fun calculateHeight() {
        val result = _poseResult.value ?: return
        val tiltFactor = tiltSensor.getTiltCorrectionFactor()

        val heightResult = heightCalculator.calculateHeight(
            result = result,
            cameraId = currentCameraId,
            tiltCorrectionFactor = tiltFactor
        )

        _calculatedResult.postValue(heightResult)
    }

    /**
     * Resets the calculated result so the fragment can navigate again on next capture.
     */
    fun resetCalculatedResult() {
        _calculatedResult.value = null
    }

    /**
     * Saves a measurement to the database.
     */
    fun saveMeasurement(name: String, heightResult: HeightResult, onComplete: () -> Unit) {
        viewModelScope.launch {
            val entity = MeasurementEntity(
                personName = name,
                heightCm = heightResult.heightCm,
                accuracyLabel = heightResult.accuracyLabel
            )
            dao.insert(entity)
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tiltSensor.stop()
    }
}

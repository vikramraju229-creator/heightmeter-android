package com.heightmeter.app.camera

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * Result of a single pose analysis frame.
 */
data class PoseAnalysisResult(
    val pose: Pose?,
    val nose: PoseLandmark?,
    val leftAnkle: PoseLandmark?,
    val rightAnkle: PoseLandmark?,
    val noseConfidence: Float,
    val ankleConfidence: Float,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Wraps ML Kit Pose Detection for real-time analysis of camera frames.
 * Uses the accurate pose detector for best height estimation.
 */
class PoseAnalyzer {

    private val detector: PoseDetector

    /** Listener called on each analyzed frame with the result. */
    var onPoseResult: ((PoseAnalysisResult) -> Unit)? = null

    init {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        detector = PoseDetection.getClient(options)
    }

    /**
     * Returns an [ImageAnalysis.Analyzer] that can be attached to a CameraX
     * ImageAnalysis use case.
     */
    fun getImageAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            processImageProxy(imageProxy)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                val result = extractResult(pose, imageProxy.width, imageProxy.height)
                onPoseResult?.invoke(result)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Extracts landmarks from a [Pose] and returns a [PoseAnalysisResult].
     */
    private fun extractResult(pose: Pose, width: Int, height: Int): PoseAnalysisResult {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val noseConfidence = nose?.inFrameLikelihood ?: 0f
        val leftConfidence = leftAnkle?.inFrameLikelihood ?: 0f
        val rightConfidence = rightAnkle?.inFrameLikelihood ?: 0f
        val ankleConfidence = (leftConfidence + rightConfidence) / 2f

        return PoseAnalysisResult(
            pose = pose,
            nose = nose,
            leftAnkle = leftAnkle,
            rightAnkle = rightAnkle,
            noseConfidence = noseConfidence,
            ankleConfidence = ankleConfidence,
            imageWidth = width,
            imageHeight = height
        )
    }

    /**
     * Releases the detector resources.
     */
    fun close() {
        detector.close()
    }
}

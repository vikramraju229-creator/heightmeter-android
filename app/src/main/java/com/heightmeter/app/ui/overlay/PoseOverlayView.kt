package com.heightmeter.app.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Custom overlay that draws the detected pose skeleton and landmark dots
 * on top of the camera preview.
 *
 * Coordinates are expected to be in the image's coordinate space, and the
 * view scales them to match its own dimensions.
 */
class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** The latest pose to draw, or null to clear. */
    var pose: Pose? = null

    /** Whether the head (nose) landmark is considered detected (confidence > 0.7). */
    var headDetected: Boolean = false

    /** Whether the ankle landmarks are detected (confidence > 0.7). */
    var feetDetected: Boolean = false

    /** Dimensions of the input image (used for coordinate scaling). */
    var imageWidth: Int = 1
    var imageHeight: Int = 1

    private val skeletonPaint = Paint().apply {
        color = Color.argb(128, 255, 255, 255)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val landmarkPaint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    private val headPaint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    private val feetPaint = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentPose = pose ?: return

        // Scale factors from image to view
        val scaleX = width.toFloat() / imageWidth.coerceAtLeast(1)
        val scaleY = height.toFloat() / imageHeight.coerceAtLeast(1)

        // Draw skeleton connections
        drawSkeleton(canvas, currentPose, scaleX, scaleY)

        // Draw all landmarks as dots
        for (landmark in currentPose.allPoseLandmarks) {
            val x = landmark.position.x * scaleX
            val y = landmark.position.y * scaleY

            when (landmark.landmarkType) {
                PoseLandmark.NOSE -> {
                    headPaint.color = if (headDetected) Color.GREEN else Color.RED
                    headPaint.alpha = if (headDetected) 255 else 128
                    canvas.drawCircle(x, y, 12f, headPaint)
                    // Draw outer ring
                    headPaint.style = Paint.Style.STROKE
                    headPaint.strokeWidth = 3f
                    canvas.drawCircle(x, y, 16f, headPaint)
                    headPaint.style = Paint.Style.FILL
                }
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE -> {
                    feetPaint.color = if (feetDetected) Color.GREEN else Color.RED
                    feetPaint.alpha = if (feetDetected) 255 else 128
                    canvas.drawCircle(x, y, 10f, feetPaint)
                    feetPaint.style = Paint.Style.STROKE
                    feetPaint.strokeWidth = 3f
                    canvas.drawCircle(x, y, 14f, feetPaint)
                    feetPaint.style = Paint.Style.FILL
                }
                else -> {
                    landmarkPaint.color = Color.argb(180, 0, 255, 255)
                    canvas.drawCircle(x, y, 6f, landmarkPaint)
                }
            }
        }
    }

    /**
     * Draws the pose skeleton connections between key landmarks.
     */
    private fun drawSkeleton(canvas: Canvas, pose: Pose, scaleX: Float, scaleY: Float) {
        // Define skeleton connections as pairs of landmark types
        val connections = listOf(
            PoseLandmark.LEFT_EAR to PoseLandmark.LEFT_EYE,
            PoseLandmark.RIGHT_EAR to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EYE to PoseLandmark.NOSE,
            PoseLandmark.RIGHT_EYE to PoseLandmark.NOSE,
            PoseLandmark.NOSE to PoseLandmark.LEFT_EAR,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EAR,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_HEEL to PoseLandmark.LEFT_FOOT_INDEX,
            PoseLandmark.RIGHT_HEEL to PoseLandmark.RIGHT_FOOT_INDEX,
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_HEEL,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_HEEL
        )

        for ((startType, endType) in connections) {
            val start = pose.getPoseLandmark(startType) ?: continue
            val end = pose.getPoseLandmark(endType) ?: continue

            val startX = start.position.x * scaleX
            val startY = start.position.y * scaleY
            val endX = end.position.x * scaleX
            val endY = end.position.y * scaleY

            canvas.drawLine(startX, startY, endX, endY, skeletonPaint)
        }
    }
}

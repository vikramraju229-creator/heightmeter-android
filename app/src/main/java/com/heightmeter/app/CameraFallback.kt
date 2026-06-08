package com.heightmeter.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.Locale
import kotlin.math.sqrt

/**
 * Camera-only fallback when ARCore is unavailable.
 * Provides 2D on-screen tap-to-measure with pixel distances.
 * Includes a calibration feature: tap "Calibrate" and enter a known real-world
 * size (e.g. "10 cm on screen = 2.5 cm real") for approximate measurements.
 */
class CameraFallback(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val overlayView: CameraOverlayView
) {
    private var cameraProvider: ProcessCameraProvider? = null

    /** Pixels per centimeter — set via calibration. 0 = uncalibrated. */
    var pixelsPerCm: Float = 0f

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            bindPreview(provider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreview(provider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview)
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    /**
     * Custom overlay View for drawing 2D measurement lines and labels
     * on top of the camera preview.
     */
    class CameraOverlayView(context: Context) : View(context) {
        private val points = mutableListOf<PointF>()
        private var calibrationPoints = mutableListOf<PointF>()

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFB800")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFB800")
            style = Paint.Style.FILL
        }
        private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1A1A2E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            isFakeBoldText = true
        }
        private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(204, 255, 184, 0)
        }

        /** Pixels per cm — set externally by [CameraFallback.pixelsPerCm] */
        var pixelsPerCm: Float = 0f

        /** Callback when points change */
        var onPointsChanged: ((List<PointF>) -> Unit)? = null

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Ignore touches near top/bottom toolbars (56dp top, 80dp bottom)
            // ~4.2px per dp on mdpi, ~8.4px on xhdpi — use generous margin
            val topBarZone = 120f
            val bottomBarZone = height - 160f
            if (event.y < topBarZone || event.y > bottomBarZone) return false

            if (event.action == MotionEvent.ACTION_UP) {
                points.add(PointF(event.x, event.y))
                invalidate()
                onPointsChanged?.invoke(points.toList())
            }
            return true
        }

        fun undoLast() {
            if (points.isNotEmpty()) {
                points.removeAt(points.size - 1)
                invalidate()
                onPointsChanged?.invoke(points.toList())
            }
        }

        fun clearAll() {
            points.clear()
            calibrationPoints.clear()
            invalidate()
            onPointsChanged?.invoke(emptyList())
        }

        fun getPoints(): List<PointF> = points.toList()

        /** Add a calibration point pair. */
        fun addCalibrationPoint(p: PointF) {
            calibrationPoints.add(p)
            if (calibrationPoints.size >= 2) {
                // Two calibration points define a known distance
                val dx = calibrationPoints[1].x - calibrationPoints[0].x
                val dy = calibrationPoints[1].y - calibrationPoints[0].y
                val pixelDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                // Store pixelDist in pixelsPerCm temporarily — caller sets actual cm
                pixelsPerCm = pixelDist
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (points.isEmpty()) {
                // Draw hint
                textPaint.textSize = 24f
                canvas.drawText("Tap points to measure (2D)", 40f, 60f, textPaint)
                textPaint.textSize = 36f
                return
            }

            // Draw lines between consecutive points
            val pointRadius = 14f
            for (i in 0 until points.size - 1) {
                canvas.drawLine(
                    points[i].x, points[i].y,
                    points[i + 1].x, points[i + 1].y,
                    linePaint
                )
            }

            // Draw all points
            for (p in points) {
                canvas.drawCircle(p.x, p.y, pointRadius, pointPaint)
                canvas.drawCircle(p.x, p.y, pointRadius, pointStrokePaint)
            }

            // Draw distance labels
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val dx = (p2.x - p1.x).toDouble()
                val dy = (p2.y - p1.y).toDouble()
                val pixelDist = sqrt(dx * dx + dy * dy).toFloat()
                val mx = (p1.x + p2.x) / 2f
                val my = (p1.y + p2.y) / 2f

                val label = if (pixelsPerCm > 0f) {
                    val cm = pixelDist / pixelsPerCm
                    String.format(Locale.US, "%.1f cm", cm)
                } else {
                    String.format(Locale.US, "%.0f px", pixelDist)
                }

                // Badge background
                val textWidth = textPaint.measureText(label)
                val pad = 16f
                val badgeH = 36f
                labelBgRect.set(
                    mx - textWidth / 2f - pad,
                    my - badgeH - 4f,
                    mx + textWidth / 2f + pad,
                    my - 4f
                )
                canvas.drawRoundRect(labelBgRect, 18f, 18f, labelBgPaint)
                canvas.drawText(label, mx - textWidth / 2f, my - 10f, textPaint)
            }

            // Show total
            if (points.size >= 2) {
                var totalPx = 0f
                for (i in 0 until points.size - 1) {
                    val dx = (points[i + 1].x - points[i].x).toDouble()
                    val dy = (points[i + 1].y - points[i].y).toDouble()
                    totalPx += sqrt(dx * dx + dy * dy).toFloat()
                }
                val totalLabel = if (pixelsPerCm > 0f) {
                    val cm = totalPx / pixelsPerCm
                    String.format(Locale.US, "Total: %.1f cm", cm)
                } else {
                    String.format(Locale.US, "Total: %.0f px", totalPx)
                }
                textPaint.textSize = 28f
                canvas.drawText(totalLabel, 40f, height - 40f, textPaint)
                textPaint.textSize = 36f
            }
        }

        companion object {
            private val labelBgRect = android.graphics.RectF()
        }
    }
}

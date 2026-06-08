package com.heightmeter.app.measurement

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.Node
import com.heightmeter.app.R
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.Point3D
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages AR rendering of measurement elements using 2D Canvas overlay.
 * Points are anchored in ARCore, but all visual elements are drawn on a 2D overlay
 * by projecting 3D coordinates to screen space.
 */
class MeasurementRenderer(
    private val context: Context,
    private val arSceneView: ARSceneView
) {
    companion object {
        private const val TAG = "MeasurementRenderer"
        private const val POINT_RADIUS_2D = 12f
        private const val LINE_WIDTH_2D = 4f
        private const val CROSSHAIR_COLOR = -0x1 // Color.WHITE
        private val MEASUREMENT_COLOR = Color.parseColor("#FFFFB800")
        private val MEASUREMENT_DARK = Color.parseColor("#FF1A1A2E")
    }

    // 3D anchor positions for projecting to screen
    private data class AnchorPoint(
        val point: Point3D,
        val anchor: Anchor
    )

    private val anchorPoints = CopyOnWriteArrayList<AnchorPoint>()
    private val lineEndpoints = CopyOnWriteArrayList<Pair<Point3D, Point3D>>()
    private val labelData = CopyOnWriteArrayList<LabelInfo>()

    // AR state
    private var session: Session? = null
    private var currentFrame: Frame? = null

    /** The latest AR frame, safe to read from UI thread after onSessionUpdated. */
    val latestFrame: Frame?
        get() = currentFrame

    // Callbacks
    var onPointPlaced: ((Point3D) -> Unit)? = null

    // Grid overlay state
    var gridEnabled: Boolean = false

    // Drawing tools
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MEASUREMENT_COLOR
        strokeWidth = LINE_WIDTH_2D
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MEASUREMENT_COLOR
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MEASUREMENT_DARK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        isFakeBoldText = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Semi-transparent yellow-orange (ARGB: 0xCCFFB800)
        color = android.graphics.Color.argb(204, 255, 184, 0)
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CROSSHAIR_COLOR
        strokeWidth = 3f
    }

    private val labelBgRect = RectF()

    // Frame timing for updates
    private var lastFrameTime = 0L

    /**
     * Initialize the renderer with an active AR session.
     */
    fun onSessionCreated(session: Session) {
        this.session = session
    }

    /**
     * Called every frame to update AR state.
     */
    fun onUpdate(frame: Frame) {
        this.currentFrame = frame
    }

    /**
     * Handle a tap event and place a measurement point.
     */
    fun handleTap(
        motionEvent: MotionEvent,
        hitResult: HitResult?
    ): Boolean {
        val frame = currentFrame ?: return false

        val tapHitResult = hitResult ?: hitTest(frame, motionEvent.x, motionEvent.y) ?: return false

        // Create an anchor at the hit position
        val anchor = try {
            tapHitResult.createAnchor()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create anchor", e)
            return false
        }

        val pose = tapHitResult.hitPose
        val point = Point3D(
            x = pose.tx(),
            y = pose.ty(),
            z = pose.tz()
        )

        anchorPoints.add(AnchorPoint(point, anchor))
        onPointPlaced?.invoke(point)
        return true
    }

    /**
     * Hit test against detected planes and feature points.
     */
    private fun hitTest(frame: Frame, x: Float, y: Float): HitResult? {
        // Try plane hit test first
        val planeHits = frame.hitTest(x, y).filter {
            it.trackable is Plane && it.trackable.trackingState == TrackingState.TRACKING
        }
        if (planeHits.isNotEmpty()) {
            return planeHits[0]
        }

        // Fall back to feature point hit test
        val pointHits = frame.hitTest(x, y).filter { hitResult ->
            hitResult.trackable is Point && hitResult.trackable.trackingState == TrackingState.TRACKING
        }
        return pointHits.firstOrNull()
    }

    /**
     * Set the line segments to be drawn.
     */
    fun setLineSegments(segments: List<Pair<Point3D, Point3D>>) {
        lineEndpoints.clear()
        lineEndpoints.addAll(segments)
    }

    /**
     * Update label data for the 2D overlay.
     */
    fun updateLabels(labels: List<LabelInfo>) {
        labelData.clear()
        labelData.addAll(labels)
    }

    /**
     * Update the line stroke width used for measurement lines.
     */
    fun setLineThickness(thickness: Float) {
        linePaint.strokeWidth = thickness
    }

    /**
     * Clear all measurement graphics.
     */
    fun clearAll() {
        anchorPoints.forEach { ap ->
            try {
                ap.anchor.detach()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detaching anchor", e)
            }
        }
        anchorPoints.clear()
        lineEndpoints.clear()
        labelData.clear()
    }

    /**
     * Remove the last placed point and clear associated lines/labels.
     * Lines will be recalculated by the ViewModel after removal.
     */
    fun removeLastPoint() {
        if (anchorPoints.isNotEmpty()) {
            val last = anchorPoints.removeAt(anchorPoints.size - 1)
            try {
                last.anchor.detach()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error detaching anchor", e)
            }
            // Clear lines and labels — they will be recalculated by ViewModel
            lineEndpoints.clear()
            labelData.clear()
        }
    }

    /**
     * Get current placed points.
     */
    fun getPlacedPoints(): List<Point3D> = anchorPoints.map { it.point }

    /**
     * Draw the 2D overlay (crosshair, lines, points, labels).
     */
    fun drawOverlay(canvas: Canvas, viewWidth: Int, viewHeight: Int) {
        // Draw crosshair in center
        drawCrosshair(canvas, viewWidth, viewHeight)

        val frame = currentFrame ?: return

        // Draw grid overlay if enabled
        if (gridEnabled) {
            drawGrid(canvas, viewWidth, viewHeight)
        }

        // Project and draw lines
        for (segment in lineEndpoints) {
            val p1 = projectToScreen(frame, segment.first)
            val p2 = projectToScreen(frame, segment.second)
            if (p1 != null && p2 != null) {
                canvas.drawLine(p1.first, p1.second, p2.first, p2.second, linePaint)
            }
        }

        // Draw point markers
        for (ap in anchorPoints) {
            val screenPos = projectToScreen(frame, ap.point)
            if (screenPos != null) {
                canvas.drawCircle(screenPos.first, screenPos.second, POINT_RADIUS_2D, pointFillPaint)
                canvas.drawCircle(screenPos.first, screenPos.second, POINT_RADIUS_2D, pointStrokePaint)
            }
        }

        // Draw labels
        for (label in labelData) {
            val screenPos = projectToScreen(frame, label.position)
            if (screenPos != null) {
                drawLabelBadge(canvas, label.text, screenPos.first, screenPos.second)
            }
        }
    }

    /**
     * Draw a simple grid overlay to help with spatial awareness.
     */
    private fun drawGrid(canvas: Canvas, width: Int, height: Int) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 255, 255, 255)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val gridSpacing = 60f
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSpacing
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSpacing
        }
    }

    /**
     * Draw a crosshair/reticle in the center of the screen.
     */
    private fun drawCrosshair(canvas: Canvas, width: Int, height: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val size = 15f
        val gap = 5f

        // Four L-shaped brackets
        // Top-left
        canvas.drawLine(cx - size, cy - gap, cx - gap, cy - gap, crosshairPaint)
        canvas.drawLine(cx - gap, cy - size, cx - gap, cy - gap, crosshairPaint)
        // Top-right
        canvas.drawLine(cx + gap, cy - gap, cx + size, cy - gap, crosshairPaint)
        canvas.drawLine(cx + gap, cy - size, cx + gap, cy - gap, crosshairPaint)
        // Bottom-left
        canvas.drawLine(cx - size, cy + gap, cx - gap, cy + gap, crosshairPaint)
        canvas.drawLine(cx - gap, cy + gap, cx - gap, cy + size, crosshairPaint)
        // Bottom-right
        canvas.drawLine(cx + gap, cy + gap, cx + size, cy + gap, crosshairPaint)
        canvas.drawLine(cx + gap, cy + gap, cx + gap, cy + size, crosshairPaint)

        // Center dot
        canvas.drawCircle(cx, cy, 3f, crosshairPaint)
    }

    /**
     * Draw a yellow rounded rectangle badge with text.
     */
    private fun drawLabelBadge(canvas: Canvas, text: String, x: Float, y: Float) {
        val textWidth = labelPaint.measureText(text)
        val padding = 20f
        val badgeHeight = 44f
        val cornerRadius = 22f

        labelBgRect.set(
            x - textWidth / 2f - padding,
            y - badgeHeight,
            x + textWidth / 2f + padding,
            y
        )
        canvas.drawRoundRect(labelBgRect, cornerRadius, cornerRadius, labelBgPaint)

        // Draw text centered in badge
        val textY = y - (badgeHeight - labelPaint.textSize) / 2f - 4f
        canvas.drawText(text, x - textWidth / 2f, textY, labelPaint)
    }

    /**
     * Project a 3D point to 2D screen coordinates.
     * Uses ARCore's camera projection with correct matrix math.
     * World → View → Clip → NDC → Screen coordinates.
     */
    private fun projectToScreen(frame: Frame, point: Point3D): Pair<Float, Float>? {
        return try {
            val viewMatrix = FloatArray(16)
            val projMatrix = FloatArray(16)
            frame.camera.getViewMatrix(viewMatrix, 0)
            frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)

            val worldX = point.x
            val worldY = point.y
            val worldZ = point.z

            // Model-view transform (world to view/eye space)
            // Column-major matrix: result[i] = sum_j(matrix[4*j + i] * input[j])
            val viewX = viewMatrix[0] * worldX + viewMatrix[4] * worldY + viewMatrix[8] * worldZ + viewMatrix[12]
            val viewY = viewMatrix[1] * worldX + viewMatrix[5] * worldY + viewMatrix[9] * worldZ + viewMatrix[13]
            val viewZ = viewMatrix[2] * worldX + viewMatrix[6] * worldY + viewMatrix[10] * worldZ + viewMatrix[14]
            val viewW = viewMatrix[3] * worldX + viewMatrix[7] * worldY + viewMatrix[11] * worldZ + viewMatrix[15]

            if (kotlin.math.abs(viewW) < 1e-10f) return null // Singular transform

            // Projection transform (view to clip space)
            val clipX = projMatrix[0] * viewX + projMatrix[4] * viewY + projMatrix[8] * viewZ + projMatrix[12] * viewW
            val clipY = projMatrix[1] * viewX + projMatrix[5] * viewY + projMatrix[9] * viewZ + projMatrix[13] * viewW
            val clipZ = projMatrix[2] * viewX + projMatrix[6] * viewY + projMatrix[10] * viewZ + projMatrix[14] * viewW
            val clipW = projMatrix[3] * viewX + projMatrix[7] * viewY + projMatrix[11] * viewZ + projMatrix[15] * viewW

            if (kotlin.math.abs(clipW) < 1e-10f) return null // Behind camera after projection

            // Perspective divide (clip to NDC)
            val ndcX = clipX / clipW
            val ndcY = clipY / clipW
            val ndcZ = clipZ / clipW

            if (ndcZ > 1f || ndcZ < -1f) return null

            val viewWidth = arSceneView.width.toFloat()
            val viewHeight = arSceneView.height.toFloat()

            // NDC to screen coordinates
            val screenX = (ndcX + 1f) * 0.5f * viewWidth
            val screenY = (1f - ndcY) * 0.5f * viewHeight

            Pair(screenX, screenY)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        clearAll()
        session = null
        currentFrame = null
    }
}

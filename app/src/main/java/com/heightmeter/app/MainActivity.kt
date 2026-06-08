package com.heightmeter.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import com.google.ar.core.ArCoreApk
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.heightmeter.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementCalculator
import com.heightmeter.app.measurement.MeasurementRenderer
import com.heightmeter.app.measurement.MeasurementViewModel
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.measurement.tools.CircleTool
import io.github.sceneview.ar.ARSceneView
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingFailureReason

/**
 * Main AR Camera Activity.
 *
 * Flow:
 * 1. Request camera permission explicitly (don't rely solely on ARSceneView's internal handling).
 * 2. Only after permission is granted, create ARSceneView programmatically with
 *    ComponentActivity reference so ARCore can manage session lifecycle.
 * 3. Show on-screen status/error messages in [textInstructions] and via Snackbar
 *    for debugging without Logcat.
 * 4. If AR session fails or tracking fails, show the reason on screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MeasurementViewModel
    private var renderer: MeasurementRenderer? = null
    private var arSceneView: ARSceneView? = null

    private var toolsBottomSheet: ToolsBottomSheet? = null
    private var overlayView: MeasurementOverlayView? = null

    /** Camera-only fallback when ARCore is not available. */
    private var cameraFallback: CameraFallback? = null
    private var cameraOverlay: CameraFallback.CameraOverlayView? = null

    /** Measurement mode */
    private enum class Mode { AR, CAMERA_FALLBACK }
    private var currentMode: Mode = Mode.AR

    /** Set to true once camera permission is granted and ARSceneView is initialised. */
    private var arInitialized = false

    /** Timeout handler to detect if AR does not start within 15 seconds. */
    private val arTimeoutRunnable = Runnable {
        if (!arInitialized) {
            showError("AR did not start within 15s. Check Logcat or try reinstalling ARCore.")
        }
    }

    // Permission launcher — requests camera, then checks AR availability
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showStatus("Camera permission granted.")
            checkArAvailability {
                showStatus("Starting AR...")
                initializeAr()
            }
        } else {
            showError("Camera permission denied. AR features will not work.")
            Snackbar.make(binding.root, R.string.camera_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system bars for fullscreen AR
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        viewModel = ViewModelProvider(this)[MeasurementViewModel::class.java]

        syncUnitIndex()
        setupUi()
        observeViewModel()
        checkCameraPermission()
    }

    // ──────────────────────────────────────────────
    //  AR Availability & Permission flow
    // ──────────────────────────────────────────────

    /**
     * Check if ARCore is available on this device.
     * If not, switch to camera-only fallback.
     */
    private fun checkArAvailability(onArAvailable: () -> Unit) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isSupported) {
                android.util.Log.d("MainActivity", "ARCore is supported")
                onArAvailable()
            } else {
                android.util.Log.w("MainActivity", "ARCore NOT supported — using CameraX fallback")
                showStatus("AR not available. Using 2D camera mode.")
                initializeCameraFallback()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "ARCore check failed", e)
            showStatus("AR check failed. Using 2D camera mode.")
            initializeCameraFallback()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                showStatus("Camera permission OK.")
                checkArAvailability {
                    showStatus("Starting AR...")
                    initializeAr()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showStatus("Camera permission needed")
                Snackbar.make(binding.root, R.string.camera_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant") { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    .show()
            }
            else -> {
                showStatus("Requesting camera permission...")
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // ──────────────────────────────────────────────
    //  AR initialisation (called after permission OK)
    // ──────────────────────────────────────────────

    private fun initializeAr() {
        if (arInitialized) return

        showStatus("Initializing AR session...")
        android.util.Log.d("MainActivity", "initializeAr() — creating ARSceneView")

        // Start a 15-second timeout for AR initialisation
        binding.root.postDelayed(arTimeoutRunnable, 15_000L)

        // ── Create ARSceneView programmatically ──
        // SceneView 2.x ARSceneView constructor: (Context, AttributeSet?, Int, Int, LifecycleOwner?)
        val sceneView = ARSceneView(
            this,       // context
            null,       // attrs
            0,          // defStyleAttr
            0,          // defStyleRes
            this        // lifecycleOwner (Activity implements LifecycleOwner)
        )
        arSceneView = sceneView

        // Add to container BEFORE binding lifecycle (so view is in hierarchy)
        sceneView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        binding.arSceneContainer.addView(sceneView, 0)

        // ── Initialize renderer ──
        renderer = MeasurementRenderer(this, sceneView)

        // ── Setup touch listener for AR scene ──
        sceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                handleTouchOnAr(event)
            }
            true
        }

        // ── AR callbacks — ALL set BEFORE lifecycle binding ──
        sceneView.sessionConfiguration = { _, config ->
            try {
                config.depthMode = com.google.ar.core.Config.DepthMode.AUTOMATIC
                config.planeFindingMode =
                    com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode =
                    com.google.ar.core.Config.LightEstimationMode.AMBIENT_INTENSITY
                android.util.Log.d("MainActivity", "Session configuration applied")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error configuring session", e)
                showError("Config error: ${e.message}")
            }
        }

        sceneView.onSessionCreated = { session ->
            arInitialized = true
            binding.root.removeCallbacks(arTimeoutRunnable)
            android.util.Log.d("MainActivity", "AR session created OK")
            showStatus("AR ready — tap to measure")
            renderer?.onSessionCreated(session)
            // Log session features
            try {
                val supported = session.isDepthModeSupported(
                    com.google.ar.core.Config.DepthMode.AUTOMATIC
                )
                android.util.Log.d("MainActivity", "Depth mode supported: $supported")
            } catch (_: Exception) {}
        }

        sceneView.onSessionUpdated = { _, frame ->
            renderer?.onUpdate(frame)
            overlayView?.invalidate()
        }

        sceneView.onSessionFailed = { exception ->
            binding.root.removeCallbacks(arTimeoutRunnable)
            android.util.Log.e("MainActivity", "AR session FAILED", exception)
            val msg = "AR session error: ${exception.message ?: "Unknown"}"
            showError(msg)
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") { initializeAr() }
                .show()
        }

        sceneView.onTrackingFailureChanged = { reason ->
            android.util.Log.w("MainActivity", "Tracking failure: $reason")
            when (reason) {
                TrackingFailureReason.NONE -> {
                    showStatus("AR ready")
                }
                TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                    showStatus("Move to brighter area")
                }
                TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                    showStatus("Point at area with more detail")
                }
                TrackingFailureReason.EXCESSIVE_MOTION -> {
                    showStatus("Move device slower")
                }
                TrackingFailureReason.BAD_STATE -> {
                    showStatus("AR tracking lost — try restarting")
                }
                TrackingFailureReason.CAMERA_UNAVAILABLE -> {
                    showError("Camera unavailable — restart app")
                }
                else -> {
                    showStatus("Tracking: $reason")
                }
            }
        }

        // ── Overlay view ──
        // Lifecycle was already passed via constructor, so ARCore starts on ON_RESUME.
        overlayView = MeasurementOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setRenderer(renderer!!)
        }
        binding.root.addView(overlayView)
    }

    // ──────────────────────────────────────────────
    //  CameraX fallback (when ARCore is unavailable)
    // ──────────────────────────────────────────────

    private fun initializeCameraFallback() {
        currentMode = Mode.CAMERA_FALLBACK
        binding.root.removeCallbacks(arTimeoutRunnable)

        // Hide AR container, show camera preview
        binding.arSceneContainer.visibility = android.view.View.GONE
        binding.fallbackPreviewView.visibility = android.view.View.VISIBLE

        // Create overlay for 2D measurements on top of camera preview
        val overlay = CameraFallback.CameraOverlayView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        cameraOverlay = overlay
        binding.root.addView(overlay)

        // Wire toolbar buttons for fallback mode
        binding.btnUndo.setOnClickListener { overlay.undoLast() }
        binding.btnClear.setOnClickListener { overlay.clearAll() }
        binding.btnAddPoint.visibility = android.view.View.GONE
        binding.btnUnitToggle.visibility = android.view.View.GONE

        // Start CameraX
        val fallback = CameraFallback(this, this, binding.fallbackPreviewView, overlay)
        cameraFallback = fallback
        fallback.start()

        showStatus("Camera mode — tap to measure (2D)")
        arInitialized = true
    }

    // ──────────────────────────────────────────────
    //  UI setup
    // ──────────────────────────────────────────────

    private fun setupUi() {
        // Toolbar buttons
        binding.btnTools.setOnClickListener { showToolsSheet() }
        binding.btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnSave.setOnClickListener { saveMeasurement() }
        binding.btnClear.setOnClickListener {
            viewModel.clearPoints()
            renderer?.clearAll()
        }
        binding.btnGalleryBottom.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.btnAddPoint.setOnClickListener { placePointAtCenter() }
        binding.btnUndo.setOnClickListener {
            viewModel.undoLastPoint()
            renderer?.removeLastPoint()
        }
        binding.btnUnitToggle.setOnClickListener { toggleUnit() }

        updateToolName(null)
    }

    private fun showStatus(msg: String) {
        binding.textInstructions.text = msg
        binding.textInstructions.visibility = View.VISIBLE
        android.util.Log.d("MainActivity", "Status: $msg")
    }

    private fun showError(msg: String) {
        binding.textInstructions.text = msg
        binding.textInstructions.visibility = View.VISIBLE
        android.util.Log.e("MainActivity", "Error: $msg")
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    // ──────────────────────────────────────────────
    //  AR interaction delegates
    // ──────────────────────────────────────────────

    private fun handleTouchOnAr(event: MotionEvent) {
        try {
            val frame = renderer?.latestFrame ?: return
            val hits = frame.hitTest(event.x, event.y)
            val planeHit = hits.firstOrNull { hit ->
                hit.trackable is Plane && hit.trackable.trackingState == TrackingState.TRACKING
            }
            val pointHit = hits.firstOrNull { hit ->
                hit.trackable is com.google.ar.core.Point && hit.trackable.trackingState == TrackingState.TRACKING
            }
            val snapToSurface = viewModel.snapToSurface.value ?: true
            val hitResult = if (snapToSurface) (planeHit ?: pointHit) else (pointHit ?: planeHit)
            if (hitResult != null) {
                renderer?.handleTap(event, hitResult)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in touch handler", e)
        }
    }

    private fun placePointAtCenter() {
        try {
            val frame = renderer?.latestFrame ?: return
            val sceneView = arSceneView ?: return
            val cx = sceneView.width / 2f
            val cy = sceneView.height / 2f
            val hits = frame.hitTest(cx, cy)
            val hitResult = hits.firstOrNull { hit ->
                hit.trackable is Plane && hit.trackable.trackingState == TrackingState.TRACKING
            } ?: hits.firstOrNull { hit ->
                hit.trackable is com.google.ar.core.Point && hit.trackable.trackingState == TrackingState.TRACKING
            }
            if (hitResult != null) {
                val motionEvent = MotionEvent.obtain(
                    android.os.SystemClock.uptimeMillis(),
                    android.os.SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN, cx, cy, 0
                )
                renderer?.handleTap(motionEvent, hitResult)
                motionEvent.recycle()
            } else {
                Snackbar.make(binding.root, R.string.error_ar_cannot_track, Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error placing point at center", e)
            Snackbar.make(binding.root, R.string.error_ar_cannot_track, Snackbar.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────
    //  ViewModel observers
    // ──────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.selectedTool.observe(this) { tool ->
            updateToolName(tool)
            updateButtons()
            binding.btnUnitToggle.visibility = if (tool != null) View.VISIBLE else View.GONE
        }
        viewModel.placedPoints.observe(this) { points ->
            updatePointsDisplay(points)
            updateButtons()
        }
        viewModel.measurementResult.observe(this) { result ->
            if (result != null) {
                renderer?.updateLabels(result.labels)
                drawMeasurementLines()
                binding.btnSave.visibility = View.VISIBLE
            } else {
                binding.btnSave.visibility = View.GONE
            }
        }
        viewModel.instructions.observe(this) { text ->
            // Only update if we're not showing a status/error message
            if (text.isNotEmpty() && arInitialized) {
                binding.textInstructions.text = text
            }
        }
        viewModel.isComplete.observe(this) { complete ->
            // Also check if a measurement result exists (for unlimited-point tools)
            val hasResult = viewModel.measurementResult.value != null
            binding.btnSave.visibility = if (complete || hasResult) View.VISIBLE else View.GONE
        }
        viewModel.selectedUnit.observe(this) { unit ->
            binding.btnUnitToggle.text = unit
        }
        viewModel.lineThickness.observe(this) { thickness ->
            renderer?.setLineThickness(thickness)
        }
        viewModel.gridEnabled.observe(this) { enabled ->
            renderer?.gridEnabled = enabled
            overlayView?.invalidate()
        }
    }

    // ──────────────────────────────────────────────
    //  Measurement helpers
    // ──────────────────────────────────────────────

    private fun updateToolName(tool: BaseTool?) {
        binding.textToolName.text = tool?.displayName ?: getString(R.string.select_tool)
    }

    private fun updateButtons() {
        val points = viewModel.placedPoints.value ?: emptyList()
        binding.btnUndo.visibility = if (points.isNotEmpty()) View.VISIBLE else View.GONE
        binding.btnClear.visibility = if (points.isNotEmpty()) View.VISIBLE else View.GONE
        binding.btnAddPoint.visibility = if (viewModel.selectedTool.value != null) View.VISIBLE else View.GONE
        binding.textPointCount.visibility = if (points.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updatePointsDisplay(points: List<Point3D>) {
        binding.textPointCount.text = "${points.size} points"
    }

    private fun drawMeasurementLines() {
        val segments = viewModel.getLineSegments()
        renderer?.setLineSegments(segments)
        val tool = viewModel.selectedTool.value
        val points = viewModel.placedPoints.value
        if (tool is CircleTool && points != null && points.size >= 2) {
            val radius = MeasurementCalculator.distance3D(points[0], points[1])
            renderer?.setLineSegments(segments + getCircleSegments(points[0], radius))
        }
    }

    private fun getCircleSegments(
        center: Point3D,
        radius: Float,
        segments: Int = 36
    ): List<Pair<Point3D, Point3D>> {
        val result = mutableListOf<Pair<Point3D, Point3D>>()
        for (i in 0 until segments) {
            val angle1 = (2f * Math.PI.toFloat() * i) / segments
            val angle2 = (2f * Math.PI.toFloat() * (i + 1)) / segments
            val p1 = Point3D(
                x = center.x + radius * kotlin.math.cos(angle1.toDouble()).toFloat(),
                y = center.y,
                z = center.z + radius * kotlin.math.sin(angle1.toDouble()).toFloat()
            )
            val p2 = Point3D(
                x = center.x + radius * kotlin.math.cos(angle2.toDouble()).toFloat(),
                y = center.y,
                z = center.z + radius * kotlin.math.sin(angle2.toDouble()).toFloat()
            )
            result.add(Pair(p1, p2))
        }
        return result
    }

    private var unitIndex = 0
    private val units = arrayOf("m", "ft", "cm", "in")

    private fun toggleUnit() {
        unitIndex = (unitIndex + 1) % units.size
        val newUnit = units[unitIndex]
        viewModel.setUnit(newUnit)
        getSharedPreferences("settings", MODE_PRIVATE)
            .edit().putString("unit", newUnit).apply()
    }

    /** Sync unitIndex to match the currently selected unit from prefs/ViewModel */
    private fun syncUnitIndex() {
        val currentUnit = viewModel.selectedUnit.value ?: "m"
        unitIndex = units.indexOf(currentUnit).coerceAtLeast(0)
    }

    private fun saveMeasurement() {
        val bitmap = takeScreenshot()
        val screenshotPath = saveScreenshotToFile(bitmap)

        // Launch coroutine to save and handle result
        lifecycleScope.launch {
            val success = viewModel.saveMeasurement(screenshotPath)
            if (success) {
                Toast.makeText(this@MainActivity, "Measurement saved!", Toast.LENGTH_SHORT).show()
                viewModel.clearPoints()
                renderer?.clearAll()
            } else {
                Toast.makeText(this@MainActivity, "Failed to save measurement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takeScreenshot(): Bitmap {
        val view = binding.arContainer
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveScreenshotToFile(bitmap: Bitmap): String? {
        return try {
            val dir = getExternalFilesDir("measurements")
                ?: java.io.File(filesDir, "measurements")
            if (!dir.exists()) dir.mkdirs()
            val filename = "measurement_${System.currentTimeMillis()}.png"
            val outputFile = java.io.File(dir, filename)
            java.io.FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outputFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun showToolsSheet() {
        if (toolsBottomSheet == null) {
            toolsBottomSheet = ToolsBottomSheet()
            toolsBottomSheet?.onToolSelected = { tool ->
                viewModel.selectTool(tool)
                renderer?.clearAll()
                unitIndex = 0
            }
        }
        toolsBottomSheet?.show(supportFragmentManager, "tools_sheet")
    }

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "onPause")
    }

    override fun onDestroy() {
        android.util.Log.d("MainActivity", "onDestroy")
        binding.root.removeCallbacks(arTimeoutRunnable)
        arSceneView?.destroy()
        renderer?.destroy()
        cameraFallback?.stop()
        super.onDestroy()
    }
}

/**
 * Custom View for drawing measurement overlays on top of the AR scene.
 */
class MeasurementOverlayView(context: Context) : View(context) {
    private var renderer: MeasurementRenderer? = null

    fun setRenderer(renderer: MeasurementRenderer) {
        this.renderer = renderer
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer?.drawOverlay(canvas, width, height)
    }
}

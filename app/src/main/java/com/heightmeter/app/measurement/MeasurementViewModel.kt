package com.heightmeter.app.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.heightmeter.app.measurement.data.Measurement
import com.heightmeter.app.measurement.data.MeasurementDatabase
import com.heightmeter.app.measurement.model.LabelInfo
import com.heightmeter.app.measurement.model.MeasurementResult
import com.heightmeter.app.measurement.model.MeasurementType
import com.heightmeter.app.measurement.model.Point3D
import com.heightmeter.app.measurement.tools.AnglesTool
import com.heightmeter.app.measurement.tools.CircleTool
import com.heightmeter.app.measurement.tools.CubeTool
import com.heightmeter.app.measurement.tools.CuboidTool
import com.heightmeter.app.measurement.tools.DistanceTool
import com.heightmeter.app.measurement.tools.HeightTool
import com.heightmeter.app.measurement.tools.LineTool
import com.heightmeter.app.measurement.tools.PolySmoothTool
import com.heightmeter.app.measurement.tools.PolygonTool
import com.heightmeter.app.measurement.tools.PolylineSmoothTool
import com.heightmeter.app.measurement.tools.PolylineTool
import com.heightmeter.app.measurement.tools.RectangleTool
import com.heightmeter.app.measurement.tools.SquareTool
import com.heightmeter.app.measurement.tools.VolumeSmoothTool
import com.heightmeter.app.measurement.tools.VolumeTool
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the AR Measurement screen.
 * Manages tool state, points, and measurement results.
 */
class MeasurementViewModel(application: Application) : AndroidViewModel(application) {

    // ── Database ──
    private val database = MeasurementDatabase.getDatabase(application)
    private val measurementDao = database.measurementDao()
    val allMeasurements: LiveData<List<Measurement>> = measurementDao.getAllMeasurements().asLiveData()

    // ── Tool Registry ──
    val availableTools: List<BaseTool> = listOf(
        LineTool(),
        HeightTool(),
        AnglesTool(),
        DistanceTool(),
        PolylineTool(),
        PolylineSmoothTool(),
        PolygonTool(),
        PolySmoothTool(),
        SquareTool(),
        RectangleTool(),
        CircleTool(),
        VolumeTool(),
        VolumeSmoothTool(),
        CuboidTool(),
        CubeTool()
    )

    // ── State ──
    private val _selectedTool = MutableLiveData<BaseTool?>(null)
    val selectedTool: LiveData<BaseTool?> = _selectedTool

    private val _placedPoints = MutableLiveData<List<Point3D>>(emptyList())
    val placedPoints: LiveData<List<Point3D>> = _placedPoints

    private val _measurementResult = MutableLiveData<MeasurementResult?>(null)
    val measurementResult: LiveData<MeasurementResult?> = _measurementResult

    private val _instructions = MutableLiveData<String>("Select a tool to start")
    val instructions: LiveData<String> = _instructions

    private val _selectedUnit = MutableLiveData("m")
    val selectedUnit: LiveData<String> = _selectedUnit

    private val _isComplete = MutableLiveData(false)
    val isComplete: LiveData<Boolean> = _isComplete

    private val _snapToSurface = MutableLiveData(true)
    val snapToSurface: LiveData<Boolean> = _snapToSurface

    private val _lineThickness = MutableLiveData(1f)
    val lineThickness: LiveData<Float> = _lineThickness

    private val _gridEnabled = MutableLiveData(false)
    val gridEnabled: LiveData<Boolean> = _gridEnabled

    // Active points list (mutable for manipulation)
    private val points = mutableListOf<Point3D>()

    // Gson for JSON serialization
    private val gson = Gson()

    // SharedPreferences listener to sync changes from SettingsActivity
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "unit" -> {
                val unit = prefs.getString("unit", "m") ?: "m"
                _selectedUnit.value = unit
                if (points.size >= (_selectedTool.value?.minPoints ?: 0)) {
                    computeResult()
                }
            }
            "snap_to_surface" -> {
                _snapToSurface.value = prefs.getBoolean("snap_to_surface", true)
            }
            "line_thickness" -> {
                _lineThickness.value = prefs.getFloat("line_thickness", 1.0f)
            }
            "grid_overlay" -> {
                _gridEnabled.value = prefs.getBoolean("grid_overlay", false)
            }
        }
    }

    init {
        // Load saved preferences
        val prefs = getApplication<Application>()
            .getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        _snapToSurface.value = prefs.getBoolean("snap_to_surface", true)
        val savedUnit = prefs.getString("unit", "m") ?: "m"
        _selectedUnit.value = savedUnit
        _lineThickness.value = prefs.getFloat("line_thickness", 1.0f)
        _gridEnabled.value = prefs.getBoolean("grid_overlay", false)

        // Listen for preference changes from SettingsActivity
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        // Set default tool to Line
        selectTool(availableTools[0])
    }

    /**
     * Select a measurement tool.
     */
    fun selectTool(tool: BaseTool) {
        _selectedTool.value = tool
        clearPoints()
        _measurementResult.value = null
        _isComplete.value = false
        _instructions.value = tool.getInstructions(0)
    }

    /**
     * Add a point from AR hit test.
     */
    fun addPoint(point: Point3D) {
        val tool = _selectedTool.value ?: return

        // Let the tool modify the point if needed
        val processedPoint = tool.onPointAdded(points, point)
        points.add(processedPoint)
        _placedPoints.value = points.toList()

        // Update instructions
        if (tool.maxPoints > 0 && points.size >= tool.maxPoints) {
            _instructions.value = "Measurement complete"
            _isComplete.value = points.size >= tool.minPoints
            if (_isComplete.value == true) {
                computeResult()
            }
        } else if (tool.maxPoints == -1 && points.size >= tool.minPoints) {
            // For unlimited-point tools, show live results and enable save
            computeResult()
            _isComplete.value = true
            _instructions.value = tool.getInstructions(points.size)
        } else {
            _instructions.value = tool.getInstructions(points.size)
        }
    }

    /**
     * Remove the last placed point.
     */
    fun undoLastPoint() {
        if (points.isNotEmpty()) {
            points.removeAt(points.size - 1)
            _placedPoints.value = points.toList()

            val tool = _selectedTool.value
            if (tool != null) {
                _instructions.value = tool.getInstructions(points.size)
            }

            if (points.size < tool?.minPoints ?: 0) {
                _measurementResult.value = null
                _isComplete.value = false
            } else {
                computeResult()
            }
        }
    }

    /**
     * Clear all points.
     */
    fun clearPoints() {
        points.clear()
        _placedPoints.value = emptyList()
        _measurementResult.value = null
        _isComplete.value = false
        val tool = _selectedTool.value
        _instructions.value = tool?.getInstructions(0) ?: "Select a tool to start"
    }

    /**
     * Compute the measurement result with current points.
     */
    private fun computeResult() {
        val tool = _selectedTool.value ?: return
        if (points.size < tool.minPoints) return

        val result = tool.calculate(points.toList(), _selectedUnit.value ?: "m")
        _measurementResult.value = result
    }

    /**
     * Get the line segments to render based on current tool and points.
     */
    fun getLineSegments(): List<Pair<Point3D, Point3D>> {
        val tool = _selectedTool.value ?: return emptyList()

        return when (tool) {
            is SquareTool -> {
                if (points.size >= 2) {
                    val corners = tool.getSquareCorners(points.toList())
                    val segments = mutableListOf<Pair<Point3D, Point3D>>()
                    for (i in 0 until 4) {
                        segments.add(Pair(corners[i], corners[(i + 1) % 4]))
                    }
                    segments
                } else emptyList()
            }
            is RectangleTool -> {
                if (points.size >= 3) {
                    val corners = tool.getRectangleCorners(points.toList())
                    val segments = mutableListOf<Pair<Point3D, Point3D>>()
                    for (i in 0 until 4) {
                        segments.add(Pair(corners[i], corners[(i + 1) % 4]))
                    }
                    segments
                } else emptyList()
            }
            is CircleTool -> {
                if (points.size >= 2) {
                    val radius = MeasurementCalculator.distance3D(points[0], points[1])
                    val circlePoints = tool.getCirclePoints(points[0], radius)
                    val segments = mutableListOf<Pair<Point3D, Point3D>>()
                    for (i in 0 until circlePoints.size - 1) {
                        segments.add(Pair(circlePoints[i], circlePoints[i + 1]))
                    }
                    segments
                } else emptyList()
            }
            else -> tool.getLineSegments(points.toList())
        }
    }

    /**
     * Get labels to display.
     */
    fun getLabels(): List<LabelInfo> {
        return _measurementResult.value?.labels ?: emptyList()
    }

    /**
     * Change the measurement unit.
     */
    fun setUnit(unit: String) {
        _selectedUnit.value = unit
        if (points.size >= (_selectedTool.value?.minPoints ?: 0)) {
            computeResult()
        }
    }

    /**
     * Set snap to surface.
     */
    fun setSnapToSurface(snap: Boolean) {
        _snapToSurface.value = snap
    }

    /**
     * Set line thickness.
     */
    fun setLineThickness(thickness: Float) {
        _lineThickness.value = thickness
    }

    /**
     * Save the current measurement to the database.
     * Returns true on success, false on failure.
     */
    suspend fun saveMeasurement(screenshotPath: String? = null): Boolean {
        val result = _measurementResult.value ?: return false
        val tool = _selectedTool.value ?: return false

        return try {
            val measurement = Measurement(
                typeName = tool.type.name,
                primaryValue = result.primaryValue.toDouble(),
                secondaryValue = result.secondaryValue?.toDouble(),
                tertiaryValue = result.tertiaryValue?.toDouble(),
                pointsJson = gson.toJson(result.points),
                screenshotPath = screenshotPath,
                unit = _selectedUnit.value ?: "m",
                label = result.labels.firstOrNull()?.text ?: ""
            )
            measurementDao.insert(measurement)
            true
        } catch (e: Exception) {
            android.util.Log.e("MeasurementViewModel", "Failed to save measurement", e)
            false
        }
    }

    /**
     * Delete a saved measurement.
     */
    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            measurementDao.delete(measurement)
        }
    }

    /**
     * Helper: parse points from JSON.
     */
    fun parsePoints(json: String): List<Point3D> {
        return try {
            val type = object : TypeToken<List<Point3D>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

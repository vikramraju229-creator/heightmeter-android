package com.heightmeter.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.heightmeter.app.data.db.AppDatabase
import com.heightmeter.app.data.db.MeasurementEntity
import com.heightmeter.app.data.model.Measurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen.
 * Loads, deletes, and shares measurements.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).measurementDao()

    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val measurements: StateFlow<List<Measurement>> = _measurements

    private val _isDeleted = MutableLiveData(false)
    val isDeleted: LiveData<Boolean> = _isDeleted

    init {
        loadMeasurements()
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            dao.getAllMeasurements().collect { entities ->
                _measurements.value = entities.map { it.toDomainModel() }
            }
        }
    }

    /**
     * Deletes a measurement by its ID.
     */
    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            dao.deleteById(measurement.id)
            _isDeleted.postValue(true)
        }
    }

    /**
     * Builds a shareable text from all measurements.
     */
    fun buildShareText(): String {
        val items = _measurements.value
        if (items.isEmpty()) return "No height measurements recorded."

        val sb = StringBuilder("📏 Height Measurements\n")
        sb.append("====================\n")
        items.forEachIndexed { index, m ->
            sb.append("${index + 1}. ${m.personName}: ${"%.1f".format(m.heightCm)} cm")
            sb.append(" (${m.toFeetInches()})")
            sb.append(" — ${m.accuracyLabel}")
            sb.append(" — ${m.formattedDate()}\n")
        }
        return sb.toString()
    }

    /**
     * Converts a database entity to a domain model.
     */
    private fun MeasurementEntity.toDomainModel(): Measurement {
        return Measurement(
            id = id,
            personName = personName,
            heightCm = heightCm,
            accuracyLabel = accuracyLabel,
            timestamp = timestamp
        )
    }
}

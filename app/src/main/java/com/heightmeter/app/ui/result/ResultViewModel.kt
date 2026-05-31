package com.heightmeter.app.ui.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.heightmeter.app.data.db.AppDatabase
import com.heightmeter.app.data.db.MeasurementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for the Result screen.
 * Handles saving measurements to the database.
 */
class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).measurementDao()

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    /**
     * Saves a new measurement to the history database.
     */
    fun saveMeasurement(name: String, heightCm: Float, accuracy: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = MeasurementEntity(
                personName = name,
                heightCm = heightCm,
                accuracyLabel = accuracy
            )
            dao.insert(entity)
            _saved.postValue(true)
        }
    }
}

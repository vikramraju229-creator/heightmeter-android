package com.heightmeter.app.measurement.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Measurement entities.
 */
@Dao
interface MeasurementDao {

    /** Get all measurements ordered by most recent first. */
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<Measurement>>

    /** Get a single measurement by ID. */
    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): Measurement?

    /** Insert a new measurement and return its ID. */
    @Insert
    suspend fun insert(measurement: Measurement): Long

    /** Update an existing measurement. */
    @Update
    suspend fun update(measurement: Measurement)

    /** Delete a measurement. */
    @Delete
    suspend fun delete(measurement: Measurement)

    /** Delete a measurement by ID. */
    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Get the count of saved measurements. */
    @Query("SELECT COUNT(*) FROM measurements")
    suspend fun getCount(): Int

    /** Delete all measurements. */
    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}

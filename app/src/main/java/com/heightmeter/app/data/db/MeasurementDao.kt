package com.heightmeter.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for height measurements.
 */
@Dao
interface MeasurementDao {

    /** Returns all measurements ordered by most recent first. */
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>

    /** Inserts a measurement and returns its row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long

    /** Deletes a measurement by ID. */
    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Deletes a measurement entity. */
    @Delete
    suspend fun delete(measurement: MeasurementEntity)
}

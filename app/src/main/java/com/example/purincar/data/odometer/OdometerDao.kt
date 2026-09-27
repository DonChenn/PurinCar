// Database reads and writes for odometer readings.
package com.example.purincar.data.odometer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface OdometerDao {
    // Adds a reading and returns its new id.
    @Insert
    suspend fun insert(reading: OdometerReadingEntity): Long

    // Replaces every field of a saved reading.
    @Update
    suspend fun update(reading: OdometerReadingEntity)

    // Removes a reading.
    @Delete
    suspend fun delete(reading: OdometerReadingEntity)

    // Watches a car's readings, newest first.
    @Query("SELECT * FROM odometer_readings WHERE carId = :carId ORDER BY date DESC, miles DESC")
    fun getReadings(carId: Long): Flow<List<OdometerReadingEntity>>

    // Reads a car's readings once, newest first.
    @Query("SELECT * FROM odometer_readings WHERE carId = :carId ORDER BY date DESC, miles DESC")
    suspend fun getReadingsOnce(carId: Long): List<OdometerReadingEntity>

    // Finds the reading already taken for a car on a given day.
    @Query("SELECT * FROM odometer_readings WHERE carId = :carId AND date = :date LIMIT 1")
    suspend fun getReadingOn(carId: Long, date: LocalDate): OdometerReadingEntity?

    // Finds the reading linked to a Firestore document.
    @Query("SELECT * FROM odometer_readings WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getReadingByFirestoreId(firestoreId: String): OdometerReadingEntity?

    // Links a reading to its Firestore document.
    @Query("UPDATE odometer_readings SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun setFirestoreId(id: Long, firestoreId: String)
}

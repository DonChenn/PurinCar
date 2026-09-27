// Database reads and writes for fill-ups.
package com.example.purincar.data.gas

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GasDao {
    // Adds a fill-up and returns its new id.
    @Insert
    suspend fun insert(record: GasRecordEntity): Long

    // Replaces every field of a saved fill-up.
    @Update
    suspend fun update(record: GasRecordEntity)

    // Removes a fill-up.
    @Delete
    suspend fun delete(record: GasRecordEntity)

    // Watches a car's fill-ups, newest first.
    @Query("SELECT * FROM gas_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecords(carId: Long): Flow<List<GasRecordEntity>>

    // Reads a car's fill-ups once, newest first.
    @Query("SELECT * FROM gas_records WHERE carId = :carId ORDER BY date DESC")
    suspend fun getRecordsOnce(carId: Long): List<GasRecordEntity>

    // Reads one saved fill-up.
    @Query("SELECT * FROM gas_records WHERE id = :id")
    suspend fun getRecordOnce(id: Long): GasRecordEntity?

    // Finds the fill-up linked to a Firestore document.
    @Query("SELECT * FROM gas_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getRecordByFirestoreId(firestoreId: String): GasRecordEntity?

    // Links a fill-up to its Firestore document.
    @Query("UPDATE gas_records SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun setFirestoreId(id: Long, firestoreId: String)
}

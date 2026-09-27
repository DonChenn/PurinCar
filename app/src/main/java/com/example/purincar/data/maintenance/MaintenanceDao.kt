// Database reads and writes for service records.
package com.example.purincar.data.maintenance

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    // Adds a service record and returns its new id.
    @Insert
    suspend fun insert(record: MaintenanceRecordEntity): Long

    // Replaces every field of a saved service record.
    @Update
    suspend fun update(record: MaintenanceRecordEntity)

    // Removes a service record.
    @Delete
    suspend fun delete(record: MaintenanceRecordEntity)

    // Watches a car's service records, newest first.
    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecords(carId: Long): Flow<List<MaintenanceRecordEntity>>

    // Reads a car's service records once, newest first.
    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    suspend fun getRecordsOnce(carId: Long): List<MaintenanceRecordEntity>

    // Watches a car's records for one kind of service, newest first.
    @Query("SELECT * FROM maintenance_records WHERE carId = :carId AND serviceType = :serviceType ORDER BY date DESC")
    fun getRecordsOfType(carId: Long, serviceType: ServiceType): Flow<List<MaintenanceRecordEntity>>

    // Reads one saved service record.
    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getRecordOnce(id: Long): MaintenanceRecordEntity?

    // Finds the service record linked to a Firestore document.
    @Query("SELECT * FROM maintenance_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getRecordByFirestoreId(firestoreId: String): MaintenanceRecordEntity?

    // Links a service record to its Firestore document.
    @Query("UPDATE maintenance_records SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun setFirestoreId(id: Long, firestoreId: String)
}

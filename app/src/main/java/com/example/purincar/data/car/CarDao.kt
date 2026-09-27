// Database reads and writes for cars.
package com.example.purincar.data.car

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CarDao {
    // Adds a car and returns its new id.
    @Insert
    suspend fun insert(car: CarEntity): Long

    // Replaces every field of a saved car.
    @Update
    suspend fun update(car: CarEntity)

    // Watches every car that hasn't been deleted.
    @Query("SELECT * FROM cars WHERE isDeleted = 0 ORDER BY name")
    fun getCars(): Flow<List<CarEntity>>

    // Reads every car that hasn't been deleted once.
    @Query("SELECT * FROM cars WHERE isDeleted = 0")
    suspend fun getCarsOnce(): List<CarEntity>

    // Watches one car.
    @Query("SELECT * FROM cars WHERE id = :id")
    fun getCar(id: Long): Flow<CarEntity?>

    // Reads one car once.
    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getCarOnce(id: Long): CarEntity?

    // Finds the car linked to a Smartcar vehicle.
    @Query("SELECT * FROM cars WHERE smartcarId = :smartcarId LIMIT 1")
    suspend fun getCarBySmartcarId(smartcarId: String): CarEntity?

    // Finds the car linked to a Firestore document.
    @Query("SELECT * FROM cars WHERE firestoreCarId = :firestoreCarId LIMIT 1")
    suspend fun getCarByFirestoreId(firestoreCarId: String): CarEntity?

    // Links a car to its Firestore document.
    @Query("UPDATE cars SET firestoreCarId = :firestoreCarId WHERE id = :id")
    suspend fun setFirestoreId(id: Long, firestoreCarId: String)

    // Soft-deletes a car so it drops out of every list.
    @Query("UPDATE cars SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    // Writes fresh Smartcar details without touching fields the sync listener owns.
    @Query(
        "UPDATE cars SET name = :name, currentMileage = :mileage, lastSyncedAt = :syncedAt, " +
            "isDeleted = 0 WHERE id = :id"
    )
    suspend fun updateFromSmartcar(id: Long, name: String, mileage: Int, syncedAt: Instant)

    // Raises a car's mileage, leaving it alone if it's already higher.
    @Query("UPDATE cars SET currentMileage = :mileage WHERE id = :id AND currentMileage < :mileage")
    suspend fun raiseMileage(id: Long, mileage: Int)

    // Records when the daily background check last ran for a car.
    @Query("UPDATE cars SET lastBackgroundCheckAt = :checkedAt WHERE id = :id")
    suspend fun updateLastBackgroundCheck(id: Long, checkedAt: Instant)
}

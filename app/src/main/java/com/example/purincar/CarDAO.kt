package com.example.purincar

import androidx.room.Dao
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<Car>>

    @Insert
    suspend fun insertCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("SELECT * FROM records WHERE carId = :carId")
    fun getRecordsForCar(carId: String): Flow<List<CarRecord>>

    @Insert
    suspend fun insertRecord(record: CarRecord)

    @Delete
    suspend fun deleteRecord(record: CarRecord)
}



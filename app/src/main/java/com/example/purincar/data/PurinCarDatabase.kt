// app/src/main/java/com/example/purincar/data/PurinCarDatabase.kt
package com.example.purincar.data

import android.content.Context
import androidx.room.*
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentMileage: Int,
    val smartcarId: String? = null,
    val firestoreCarId: String? = null,
    val isDeleted: Boolean = false,
    val lastSyncedAt: Long? = null,
    val lastBackgroundCheckAt: Long? = null
)

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [ForeignKey(
        entity = CarEntity::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val serviceType: String,
    val date: String,
    val mileageAtService: Int,
    val description: String = "",
    val cost: Double = 0.0,
    val firestoreId: String? = null
)

@Entity(
    tableName = "gas_records",
    foreignKeys = [ForeignKey(
        entity = CarEntity::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GasRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val date: String,
    val gallons: Double,
    val totalCost: Double,
    val notes: String = "",
    val firestoreId: String? = null
)

@Dao
interface CarDao {
    @Insert
    suspend fun insertCar(car: CarEntity): Long

    @Update
    suspend fun updateCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("SELECT * FROM cars WHERE smartcarId = :smartcarId LIMIT 1")
    suspend fun getCarBySmartcarId(smartcarId: String): CarEntity?

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    suspend fun getCarById(id: Int): CarEntity?

    @Query("SELECT * FROM cars WHERE firestoreCarId = :firestoreCarId LIMIT 1")
    suspend fun getCarByFirestoreId(firestoreCarId: String): CarEntity?

    @Query("SELECT * FROM cars WHERE isDeleted = 0")
    fun getAllCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE isDeleted = 0")
    suspend fun getAllCarsOnce(): List<CarEntity>

    @Query("UPDATE cars SET lastBackgroundCheckAt = :timestamp WHERE id = :carId")
    suspend fun updateLastBackgroundCheck(carId: Int, timestamp: Long)

    @Query("UPDATE cars SET currentMileage = :mileage, lastSyncedAt = :syncedAt WHERE id = :carId")
    suspend fun updateMileageAndSync(carId: Int, mileage: Int, syncedAt: Long)

    @Insert
    suspend fun insertRecord(record: MaintenanceRecord): Long

    @Update
    suspend fun updateRecord(record: MaintenanceRecord)

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    suspend fun getRecordsForCarOnce(carId: Int): List<MaintenanceRecord>

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId AND serviceType = :serviceType ORDER BY date DESC")
    fun getRecordsForType(carId: Int, serviceType: String): Flow<List<MaintenanceRecord>>

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)

    @Query("SELECT * FROM maintenance_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getRecordByFirestoreId(firestoreId: String): MaintenanceRecord?

    @Insert
    suspend fun insertGasRecord(record: GasRecord): Long

    @Update
    suspend fun updateGasRecord(record: GasRecord)

    @Delete
    suspend fun deleteGasRecord(record: GasRecord)

    @Query("SELECT * FROM gas_records WHERE carId = :carId ORDER BY date DESC")
    fun getGasRecordsForCar(carId: Int): Flow<List<GasRecord>>

    @Query("SELECT * FROM gas_records WHERE carId = :carId ORDER BY date DESC")
    suspend fun getGasRecordsForCarOnce(carId: Int): List<GasRecord>

    @Query("SELECT * FROM gas_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getGasRecordByFirestoreId(firestoreId: String): GasRecord?
}


@Database(entities = [CarEntity::class, MaintenanceRecord::class, GasRecord::class], version = 12)
abstract class PurinCarDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao

    companion object {
        @Volatile
        private var Instance: PurinCarDatabase? = null

        fun getDatabase(context: Context): PurinCarDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PurinCarDatabase::class.java, "purin_car_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

// app/src/main/java/com/example/purincar/data/PurinCarDatabase.kt
package com.example.purincar.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentMileage: Int,
    val smartcarId: String? = null,

    // NEW FIELDS
    val fuelPercent: Double? = null,
    val range: Double? = null,
    val oilLife: Double? = null,
    val tirePressure: String? = null,
    val isLocked: Boolean? = null
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
    val description: String = ""
)

@Dao
interface CarDao {
    @Insert
    suspend fun insertCar(car: CarEntity)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    // Helper to find existing car so we update it instead of creating duplicates
    @Query("SELECT * FROM cars WHERE smartcarId = :smartcarId LIMIT 1")
    suspend fun getCarBySmartcarId(smartcarId: String): CarEntity?

    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<CarEntity>>

    @Insert
    suspend fun insertRecord(record: MaintenanceRecord)

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId AND serviceType = :serviceType ORDER BY date DESC")
    fun getRecordsForType(carId: Int, serviceType: String): Flow<List<MaintenanceRecord>>

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)
}

@Database(entities = [CarEntity::class, MaintenanceRecord::class], version = 4) // <--- Version 4
abstract class PurinCarDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao

    companion object {
        @Volatile
        private var Instance: PurinCarDatabase? = null

        fun getDatabase(context: Context): PurinCarDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PurinCarDatabase::class.java, "purin_car_db")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

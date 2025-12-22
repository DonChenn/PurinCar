package com.example.purincar.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentMileage: Int
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
    val mileageAtService: Int
)

@Dao
interface CarDao {
    @Insert
    suspend fun insertCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<CarEntity>>

    @Insert
    suspend fun insertRecord(record: MaintenanceRecord)

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>>
}

@Database(entities = [CarEntity::class, MaintenanceRecord::class], version = 1)
abstract class PurinCarDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao

    companion object {
        @Volatile
        private var Instance: PurinCarDatabase? = null

        fun getDatabase(context: Context): PurinCarDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PurinCarDatabase::class.java, "purin_car_db")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
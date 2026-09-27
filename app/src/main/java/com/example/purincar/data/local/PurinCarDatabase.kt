// The on-device Room database every screen reads from, and how its dates and service types are stored.
package com.example.purincar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.purincar.data.car.CarDao
import com.example.purincar.data.car.CarEntity
import com.example.purincar.data.gas.GasDao
import com.example.purincar.data.gas.GasRecordEntity
import com.example.purincar.data.maintenance.MaintenanceDao
import com.example.purincar.data.maintenance.MaintenanceRecordEntity
import com.example.purincar.data.maintenance.ServiceType
import com.example.purincar.data.odometer.OdometerDao
import com.example.purincar.data.odometer.OdometerReadingEntity
import java.time.Instant
import java.time.LocalDate

@Database(
    entities = [
        CarEntity::class,
        MaintenanceRecordEntity::class,
        GasRecordEntity::class,
        OdometerReadingEntity::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PurinCarDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun gasDao(): GasDao
    abstract fun odometerDao(): OdometerDao

    companion object {
        // Opens the database, wiping it on schema changes since Firestore refills it.
        fun create(context: Context): PurinCarDatabase =
            Room.databaseBuilder(context, PurinCarDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        private const val DATABASE_NAME = "purin_car_db"
    }
}

class Converters {
    // Stores a date as its ISO text.
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    // Reads a date back from its ISO text.
    @TypeConverter
    fun toLocalDate(text: String?): LocalDate? = text?.let(LocalDate::parse)

    // Stores a moment as epoch milliseconds.
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    // Reads a moment back from epoch milliseconds.
    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    // Stores a service type by its stable key.
    @TypeConverter
    fun fromServiceType(type: ServiceType): String = type.name

    // Reads a service type back from its stable key.
    @TypeConverter
    fun toServiceType(name: String): ServiceType = ServiceType.valueOf(name)
}

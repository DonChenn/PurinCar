package com.example.purincar

import android.R
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val year: Int? = null,
    val make: String? = null,
    val model: String? = null,
)

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class CarRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carId: String,
    val type: RecordType,
    val date: LocalDate,
    val mileage: Int,
    val notes: String? = null
)

enum class RecordType(val displayName: String) {
    AirFilter("Air Filters"),
    BatteryFan("Battery Fan"),
    BrakeFluid("Brake Fluid"),
    BrakePads("Brake Pads"),
    Coolant("Coolant"),
    EngineOil("Engine Oil"),
    SparkPlugs("Spark Plugs"),
    TransmissionFluid("Transmission Fluid")
}

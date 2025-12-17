package com.example.purincar

import android.R
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val year: Int? = null,
    val make: String? = null,
    val model: String? = null,
)

@Entity(tableName = "records")
data class CarRecord(
    val carId: String,
    val type: RecordType,
    val date: LocalDate,
    val mileage: Int,
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

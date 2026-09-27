// An odometer reading as stored in the on-device database.
package com.example.purincar.data.odometer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.purincar.data.car.CarEntity
import java.time.LocalDate

@Entity(
    tableName = "odometer_readings",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId", "date"), Index("firestoreId")]
)
data class OdometerReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val miles: Int,
    val date: LocalDate,
    val firestoreId: String? = null
)

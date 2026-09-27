// A service record as stored in the on-device database.
package com.example.purincar.data.maintenance

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.purincar.data.car.CarEntity
import java.time.LocalDate

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("firestoreId")]
)
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val serviceType: ServiceType,
    val date: LocalDate,
    val mileageAtService: Int,
    val description: String = "",
    val cost: Double = 0.0,
    val firestoreId: String? = null
)

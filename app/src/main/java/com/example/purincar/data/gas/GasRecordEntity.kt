// A fill-up as stored in the on-device database.
package com.example.purincar.data.gas

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.purincar.data.car.CarEntity
import java.time.LocalDate

@Entity(
    tableName = "gas_records",
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
data class GasRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: LocalDate,
    val gallons: Double,
    val totalCost: Double,
    val notes: String = "",
    val firestoreId: String? = null
)

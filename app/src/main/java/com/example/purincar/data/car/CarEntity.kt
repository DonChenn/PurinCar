// A car as stored in the on-device database.
package com.example.purincar.data.car

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "cars",
    indices = [Index("smartcarId"), Index("firestoreCarId")]
)
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentMileage: Int,
    val smartcarId: String? = null,
    val firestoreCarId: String? = null,
    val isDeleted: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val lastBackgroundCheckAt: Instant? = null
)

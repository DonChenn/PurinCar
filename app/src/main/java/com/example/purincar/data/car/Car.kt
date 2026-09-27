// The car model the screens show and how it's built from the database row.
package com.example.purincar.data.car

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Car(
    val id: Long,
    val name: String,
    val currentMileage: Int,
    val isSmartcarLinked: Boolean,
    val lastSyncedAt: Instant?,
    val lastBackgroundCheckAt: Instant?
)

// Converts a saved car row into the model the screens use.
fun CarEntity.toModel() = Car(
    id = id,
    name = name,
    currentMileage = currentMileage,
    isSmartcarLinked = smartcarId != null,
    lastSyncedAt = lastSyncedAt,
    lastBackgroundCheckAt = lastBackgroundCheckAt
)

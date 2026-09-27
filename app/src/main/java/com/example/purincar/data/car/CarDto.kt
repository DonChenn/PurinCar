// A car exactly as it's stored in Firestore.
package com.example.purincar.data.car

import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

data class CarDto(
    val name: String,
    val currentMileage: Int,
    val smartcarId: String?,
    val isDeleted: Boolean,
    val lastSyncedAt: Long?,
    val lastBackgroundCheckAt: Long?
)

// Reads a car document, or returns null if it has no name.
fun DocumentSnapshot.toCarDto(): CarDto? {
    val name = getString(CarFields.NAME) ?: return null
    return CarDto(
        name = name,
        currentMileage = (getLong(CarFields.CURRENT_MILEAGE) ?: 0L).toInt(),
        smartcarId = getString(CarFields.SMARTCAR_ID),
        isDeleted = getBoolean(CarFields.IS_DELETED) ?: false,
        lastSyncedAt = getLong(CarFields.LAST_SYNCED_AT),
        lastBackgroundCheckAt = getLong(CarFields.LAST_BACKGROUND_CHECK_AT)
    )
}

// Writes a car in the shape Firestore stores it.
fun CarDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    CarFields.NAME to name,
    CarFields.CURRENT_MILEAGE to currentMileage,
    CarFields.SMARTCAR_ID to smartcarId,
    CarFields.IS_DELETED to isDeleted,
    CarFields.LAST_SYNCED_AT to lastSyncedAt,
    CarFields.LAST_BACKGROUND_CHECK_AT to lastBackgroundCheckAt
)

// Converts a saved car row into its Firestore shape.
fun CarEntity.toDto() = CarDto(
    name = name,
    currentMileage = currentMileage,
    smartcarId = smartcarId,
    isDeleted = isDeleted,
    lastSyncedAt = lastSyncedAt?.toEpochMilli(),
    lastBackgroundCheckAt = lastBackgroundCheckAt?.toEpochMilli()
)

// Converts a car document into a new database row linked to that document.
fun CarDto.toEntity(firestoreCarId: String) = CarEntity(
    name = name,
    currentMileage = currentMileage,
    smartcarId = smartcarId,
    firestoreCarId = firestoreCarId,
    isDeleted = isDeleted,
    lastSyncedAt = lastSyncedAt?.let(Instant::ofEpochMilli),
    lastBackgroundCheckAt = lastBackgroundCheckAt?.let(Instant::ofEpochMilli)
)

object CarFields {
    const val NAME = "name"
    const val CURRENT_MILEAGE = "currentMileage"
    const val SMARTCAR_ID = "smartcarId"
    const val IS_DELETED = "isDeleted"
    const val LAST_SYNCED_AT = "lastSyncedAt"
    const val LAST_BACKGROUND_CHECK_AT = "lastBackgroundCheckAt"
}

// The odometer reading model, its Firestore shape and how each is converted.
package com.example.purincar.data.odometer

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

@Immutable
data class OdometerReading(
    val date: LocalDate,
    val miles: Int
)

// Converts a saved reading row into the model the screens use.
fun OdometerReadingEntity.toModel() = OdometerReading(date = date, miles = miles)

data class OdometerReadingDto(
    val miles: Int,
    val date: String
)

// Reads a reading document, or returns null if its date is missing.
fun DocumentSnapshot.toOdometerReadingDto(): OdometerReadingDto? {
    val date = getString(OdometerFields.DATE) ?: return null
    return OdometerReadingDto(miles = (getLong(OdometerFields.MILES) ?: 0L).toInt(), date = date)
}

// Writes a reading in the shape Firestore stores it.
fun OdometerReadingDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    OdometerFields.MILES to miles,
    OdometerFields.DATE to date
)

// Converts a saved reading row into its Firestore shape.
fun OdometerReadingEntity.toDto() = OdometerReadingDto(miles = miles, date = date.toString())

// Converts a reading document into a database row, or null if its date can't be read.
fun OdometerReadingDto.toEntity(id: Long, carId: Long, firestoreId: String): OdometerReadingEntity? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return OdometerReadingEntity(id = id, carId = carId, miles = miles, date = parsedDate, firestoreId = firestoreId)
}

object OdometerFields {
    const val MILES = "miles"
    const val DATE = "date"
}

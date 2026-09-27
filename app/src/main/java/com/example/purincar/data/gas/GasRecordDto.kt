// A fill-up exactly as it's stored in Firestore.
package com.example.purincar.data.gas

import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

data class GasRecordDto(
    val date: String,
    val gallons: Double,
    val totalCost: Double,
    val notes: String
)

// Reads a fill-up document, or returns null if its date is missing.
fun DocumentSnapshot.toGasRecordDto(): GasRecordDto? {
    val date = getString(GasFields.DATE) ?: return null
    return GasRecordDto(
        date = date,
        gallons = getDouble(GasFields.GALLONS) ?: 0.0,
        totalCost = getDouble(GasFields.TOTAL_COST) ?: 0.0,
        notes = getString(GasFields.NOTES).orEmpty()
    )
}

// Writes a fill-up in the shape Firestore stores it.
fun GasRecordDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    GasFields.DATE to date,
    GasFields.GALLONS to gallons,
    GasFields.TOTAL_COST to totalCost,
    GasFields.NOTES to notes
)

// Converts a saved fill-up row into its Firestore shape.
fun GasRecordEntity.toDto() = GasRecordDto(
    date = date.toString(),
    gallons = gallons,
    totalCost = totalCost,
    notes = notes
)

// Converts a fill-up document into a database row, or null if its date can't be read.
fun GasRecordDto.toEntity(id: Long, carId: Long, firestoreId: String): GasRecordEntity? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return GasRecordEntity(
        id = id,
        carId = carId,
        date = parsedDate,
        gallons = gallons,
        totalCost = totalCost,
        notes = notes,
        firestoreId = firestoreId
    )
}

object GasFields {
    const val DATE = "date"
    const val GALLONS = "gallons"
    const val TOTAL_COST = "totalCost"
    const val NOTES = "notes"
}

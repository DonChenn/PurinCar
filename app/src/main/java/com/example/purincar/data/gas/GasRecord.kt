// The fill-up model the screens show and how it's built from the database row.
package com.example.purincar.data.gas

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class GasRecord(
    val id: Long = 0,
    val carId: Long,
    val date: LocalDate,
    val gallons: Double,
    val totalCost: Double,
    val notes: String = ""
)

// Converts a saved fill-up row into the model the screens use.
fun GasRecordEntity.toModel() = GasRecord(
    id = id,
    carId = carId,
    date = date,
    gallons = gallons,
    totalCost = totalCost,
    notes = notes
)

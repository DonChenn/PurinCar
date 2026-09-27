// The service record model the screens show and how it's built from the database row.
package com.example.purincar.data.maintenance

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class MaintenanceRecord(
    val id: Long = 0,
    val carId: Long,
    val serviceType: ServiceType,
    val date: LocalDate,
    val mileageAtService: Int,
    val description: String = "",
    val cost: Double = 0.0
)

// Converts a saved service record row into the model the screens use.
fun MaintenanceRecordEntity.toModel() = MaintenanceRecord(
    id = id,
    carId = carId,
    serviceType = serviceType,
    date = date,
    mileageAtService = mileageAtService,
    description = description,
    cost = cost
)

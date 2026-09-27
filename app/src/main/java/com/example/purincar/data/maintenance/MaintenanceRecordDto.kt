// A service record exactly as it's stored in Firestore.
package com.example.purincar.data.maintenance

import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

data class MaintenanceRecordDto(
    val serviceType: String,
    val date: String,
    val mileageAtService: Int,
    val description: String,
    val cost: Double
)

// Reads a service record document, or returns null if its type or date is missing.
fun DocumentSnapshot.toMaintenanceRecordDto(): MaintenanceRecordDto? {
    val serviceType = getString(MaintenanceFields.SERVICE_TYPE) ?: return null
    val date = getString(MaintenanceFields.DATE) ?: return null
    return MaintenanceRecordDto(
        serviceType = serviceType,
        date = date,
        mileageAtService = (getLong(MaintenanceFields.MILEAGE_AT_SERVICE) ?: 0L).toInt(),
        description = getString(MaintenanceFields.DESCRIPTION).orEmpty(),
        cost = getDouble(MaintenanceFields.COST) ?: 0.0
    )
}

// Writes a service record in the shape Firestore stores it.
fun MaintenanceRecordDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    MaintenanceFields.SERVICE_TYPE to serviceType,
    MaintenanceFields.DATE to date,
    MaintenanceFields.MILEAGE_AT_SERVICE to mileageAtService,
    MaintenanceFields.DESCRIPTION to description,
    MaintenanceFields.COST to cost
)

// Converts a saved service record row into its Firestore shape.
fun MaintenanceRecordEntity.toDto() = MaintenanceRecordDto(
    serviceType = serviceType.label,
    date = date.toString(),
    mileageAtService = mileageAtService,
    description = description,
    cost = cost
)

// Converts a service record document into a database row, or null if its type or date can't be read.
fun MaintenanceRecordDto.toEntity(id: Long, carId: Long, firestoreId: String): MaintenanceRecordEntity? {
    val type = ServiceType.fromLabel(serviceType) ?: return null
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return MaintenanceRecordEntity(
        id = id,
        carId = carId,
        serviceType = type,
        date = parsedDate,
        mileageAtService = mileageAtService,
        description = description,
        cost = cost,
        firestoreId = firestoreId
    )
}

object MaintenanceFields {
    const val SERVICE_TYPE = "serviceType"
    const val DATE = "date"
    const val MILEAGE_AT_SERVICE = "mileageAtService"
    const val DESCRIPTION = "description"
    const val COST = "cost"
}

// Loads and saves service records on the device first, then copies the change to Firestore.
package com.example.purincar.data.maintenance

import com.example.purincar.data.sync.CloudPaths
import com.example.purincar.data.sync.logFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaintenanceRepository(
    private val dao: MaintenanceDao,
    private val cloud: CloudPaths
) {
    // Watches every service record for a car.
    fun records(carId: Long): Flow<List<MaintenanceRecord>> =
        dao.getRecords(carId).map { records -> records.map(MaintenanceRecordEntity::toModel) }

    // Watches a car's records for one kind of service.
    fun recordsOfType(carId: Long, type: ServiceType): Flow<List<MaintenanceRecord>> =
        dao.getRecordsOfType(carId, type).map { records -> records.map(MaintenanceRecordEntity::toModel) }

    // Reads every service record for a car once.
    suspend fun recordsOnce(carId: Long): List<MaintenanceRecord> =
        dao.getRecordsOnce(carId).map(MaintenanceRecordEntity::toModel)

    // Saves a new service record and uploads it.
    suspend fun add(record: MaintenanceRecord) {
        val entity = MaintenanceRecordEntity(
            carId = record.carId,
            serviceType = record.serviceType,
            date = record.date,
            mileageAtService = record.mileageAtService,
            description = record.description,
            cost = record.cost
        )
        val id = dao.insert(entity)
        val doc = cloud.recordsFor(record.carId, CloudPaths.MAINTENANCE_RECORDS)?.document() ?: return
        dao.setFirestoreId(id, doc.id)
        doc.set(entity.toDto().toFirestoreMap()).logFailure(TAG, "service record insert")
    }

    // Saves several new service records, as when importing a CSV file.
    suspend fun addAll(records: List<MaintenanceRecord>) {
        records.forEach { add(it) }
    }

    // Saves changes to a service record and uploads them.
    suspend fun update(record: MaintenanceRecord) {
        val existing = dao.getRecordOnce(record.id) ?: return
        val updated = existing.copy(
            serviceType = record.serviceType,
            date = record.date,
            mileageAtService = record.mileageAtService,
            description = record.description,
            cost = record.cost
        )
        dao.update(updated)
        val firestoreId = updated.firestoreId ?: return
        cloud.recordsFor(updated.carId, CloudPaths.MAINTENANCE_RECORDS)
            ?.document(firestoreId)
            ?.set(updated.toDto().toFirestoreMap())
            ?.logFailure(TAG, "service record update")
    }

    // Removes a service record here and in the cloud.
    suspend fun delete(record: MaintenanceRecord) {
        val existing = dao.getRecordOnce(record.id) ?: return
        dao.delete(existing)
        val firestoreId = existing.firestoreId ?: return
        cloud.recordsFor(existing.carId, CloudPaths.MAINTENANCE_RECORDS)
            ?.document(firestoreId)
            ?.delete()
            ?.logFailure(TAG, "service record delete")
    }

    private companion object {
        const val TAG = "MaintenanceRepository"
    }
}

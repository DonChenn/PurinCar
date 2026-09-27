// Loads and saves fill-ups on the device first, then copies the change to Firestore.
package com.example.purincar.data.gas

import com.example.purincar.data.sync.CloudPaths
import com.example.purincar.data.sync.logFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GasRepository(
    private val dao: GasDao,
    private val cloud: CloudPaths
) {
    // Watches every fill-up for a car.
    fun records(carId: Long): Flow<List<GasRecord>> =
        dao.getRecords(carId).map { records -> records.map(GasRecordEntity::toModel) }

    // Reads every fill-up for a car once.
    suspend fun recordsOnce(carId: Long): List<GasRecord> =
        dao.getRecordsOnce(carId).map(GasRecordEntity::toModel)

    // Saves a new fill-up and uploads it.
    suspend fun add(record: GasRecord) {
        val entity = GasRecordEntity(
            carId = record.carId,
            date = record.date,
            gallons = record.gallons,
            totalCost = record.totalCost,
            notes = record.notes
        )
        val id = dao.insert(entity)
        val doc = cloud.recordsFor(record.carId, CloudPaths.GAS_RECORDS)?.document() ?: return
        dao.setFirestoreId(id, doc.id)
        doc.set(entity.toDto().toFirestoreMap()).logFailure(TAG, "fill-up insert")
    }

    // Saves several new fill-ups, as when importing a CSV file.
    suspend fun addAll(records: List<GasRecord>) {
        records.forEach { add(it) }
    }

    // Saves changes to a fill-up and uploads them.
    suspend fun update(record: GasRecord) {
        val existing = dao.getRecordOnce(record.id) ?: return
        val updated = existing.copy(
            date = record.date,
            gallons = record.gallons,
            totalCost = record.totalCost,
            notes = record.notes
        )
        dao.update(updated)
        val firestoreId = updated.firestoreId ?: return
        cloud.recordsFor(updated.carId, CloudPaths.GAS_RECORDS)
            ?.document(firestoreId)
            ?.set(updated.toDto().toFirestoreMap())
            ?.logFailure(TAG, "fill-up update")
    }

    // Removes a fill-up here and in the cloud.
    suspend fun delete(record: GasRecord) {
        val existing = dao.getRecordOnce(record.id) ?: return
        dao.delete(existing)
        val firestoreId = existing.firestoreId ?: return
        cloud.recordsFor(existing.carId, CloudPaths.GAS_RECORDS)
            ?.document(firestoreId)
            ?.delete()
            ?.logFailure(TAG, "fill-up delete")
    }

    private companion object {
        const val TAG = "GasRepository"
    }
}

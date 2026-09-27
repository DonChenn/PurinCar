// Keeps one odometer reading per car per day on the device and in Firestore.
package com.example.purincar.data.odometer

import com.example.purincar.data.sync.CloudPaths
import com.example.purincar.data.sync.logFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class OdometerRepository(
    private val dao: OdometerDao,
    private val cloud: CloudPaths
) {
    // Watches a car's odometer history, newest first.
    fun readings(carId: Long): Flow<List<OdometerReading>> =
        dao.getReadings(carId).map { readings -> readings.map(OdometerReadingEntity::toModel) }

    // Records a reading for the day, replacing that day's earlier reading instead of adding a duplicate.
    suspend fun record(carId: Long, miles: Int, date: LocalDate = LocalDate.now()) {
        val existing = dao.getReadingOn(carId, date)
        if (existing != null) {
            val updated = existing.copy(miles = miles)
            dao.update(updated)
            val firestoreId = updated.firestoreId ?: return
            cloud.recordsFor(carId, CloudPaths.ODOMETER_READINGS)
                ?.document(firestoreId)
                ?.set(updated.toDto().toFirestoreMap())
                ?.logFailure(TAG, "odometer update")
            return
        }

        val entity = OdometerReadingEntity(carId = carId, miles = miles, date = date)
        val id = dao.insert(entity)
        val doc = cloud.recordsFor(carId, CloudPaths.ODOMETER_READINGS)?.document() ?: return
        dao.setFirestoreId(id, doc.id)
        doc.set(entity.toDto().toFirestoreMap()).logFailure(TAG, "odometer insert")
    }

    private companion object {
        const val TAG = "OdometerRepository"
    }
}

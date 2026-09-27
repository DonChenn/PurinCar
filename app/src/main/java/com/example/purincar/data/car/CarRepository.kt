// Loads and saves cars on the device first, then copies the change to Firestore.
package com.example.purincar.data.car

import android.util.Log
import com.example.purincar.data.sync.CloudPaths
import com.example.purincar.data.sync.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant

class CarRepository(
    private val dao: CarDao,
    private val cloud: CloudPaths
) {
    // Watches every car in the garage.
    fun cars(): Flow<List<Car>> = dao.getCars().map { cars -> cars.map(CarEntity::toModel) }

    // Watches one car, or null once it's gone.
    fun car(id: Long): Flow<Car?> = dao.getCar(id).map { it?.takeUnless(CarEntity::isDeleted)?.toModel() }

    // Reads every car in the garage once.
    suspend fun carsOnce(): List<Car> = dao.getCarsOnce().map(CarEntity::toModel)

    // Soft-deletes a car here and in the cloud so other devices hide it too.
    suspend fun delete(id: Long) {
        val car = dao.getCarOnce(id) ?: return
        dao.markDeleted(id)
        val firestoreCarId = car.firestoreCarId ?: return
        cloud.cars()?.document(firestoreCarId)?.update(CarFields.IS_DELETED, true)?.logFailure(TAG, "car delete")
    }

    // Saves the latest Smartcar name and mileage, adding the car if it's new and adding is allowed.
    suspend fun upsertFromSmartcar(
        smartcarId: String,
        name: String,
        miles: Int,
        syncedAt: Instant,
        createIfMissing: Boolean
    ): Car? {
        val existing = dao.getCarBySmartcarId(smartcarId)
        if (existing != null) {
            dao.updateFromSmartcar(existing.id, name, miles, syncedAt)
            existing.firestoreCarId?.let { firestoreCarId ->
                cloud.cars()?.document(firestoreCarId)
                    ?.update(
                        mapOf(
                            CarFields.NAME to name,
                            CarFields.CURRENT_MILEAGE to miles,
                            CarFields.LAST_SYNCED_AT to syncedAt.toEpochMilli(),
                            CarFields.IS_DELETED to false
                        )
                    )
                    ?.logFailure(TAG, "car update")
            }
            return dao.getCarOnce(existing.id)?.toModel()
        }
        if (!createIfMissing) return null

        val entity = CarEntity(name = name, currentMileage = miles, smartcarId = smartcarId, lastSyncedAt = syncedAt)
        val id = dao.insert(entity)
        uploadNewCar(entity.copy(id = id))
        return dao.getCarOnce(id)?.toModel()
    }

    // Raises a car's mileage to a newly seen reading, here and in the cloud.
    suspend fun raiseMileage(id: Long, miles: Int) {
        dao.raiseMileage(id, miles)
        val car = dao.getCarOnce(id) ?: return
        val firestoreCarId = car.firestoreCarId ?: return
        cloud.cars()?.document(firestoreCarId)
            ?.update(CarFields.CURRENT_MILEAGE, car.currentMileage)
            ?.logFailure(TAG, "mileage update")
    }

    // Stamps when the daily background check last looked at a car.
    suspend fun markBackgroundCheck(id: Long, checkedAt: Instant) {
        dao.updateLastBackgroundCheck(id, checkedAt)
    }

    // Creates the cloud copy of a new car and links it only after Firestore accepts it.
    private suspend fun uploadNewCar(car: CarEntity) {
        val doc = cloud.cars()?.document() ?: return
        try {
            doc.set(car.toDto().toFirestoreMap()).await()
            dao.setFirestoreId(car.id, doc.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firestore car insert failed; the next sign-in will retry", e)
        }
    }

    private companion object {
        const val TAG = "CarRepository"
    }
}

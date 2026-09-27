// Keeps the on-device database and the signed-in user's Firestore garage in step while someone is signed in.
package com.example.purincar.data.sync

import android.util.Log
import com.example.purincar.data.car.CarDao
import com.example.purincar.data.car.CarEntity
import com.example.purincar.data.car.toCarDto
import com.example.purincar.data.car.toDto
import com.example.purincar.data.car.toEntity
import com.example.purincar.data.car.toFirestoreMap
import com.example.purincar.data.gas.GasDao
import com.example.purincar.data.gas.toDto
import com.example.purincar.data.gas.toEntity
import com.example.purincar.data.gas.toFirestoreMap
import com.example.purincar.data.gas.toGasRecordDto
import com.example.purincar.data.maintenance.MaintenanceDao
import com.example.purincar.data.maintenance.toDto
import com.example.purincar.data.maintenance.toEntity
import com.example.purincar.data.maintenance.toFirestoreMap
import com.example.purincar.data.maintenance.toMaintenanceRecordDto
import com.example.purincar.data.odometer.OdometerDao
import com.example.purincar.data.odometer.toDto
import com.example.purincar.data.odometer.toEntity
import com.example.purincar.data.odometer.toFirestoreMap
import com.example.purincar.data.odometer.toOdometerReadingDto
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Collections

class FirestoreSync(
    private val cloud: CloudPaths,
    private val carDao: CarDao,
    private val maintenanceDao: MaintenanceDao,
    private val gasDao: GasDao,
    private val odometerDao: OdometerDao,
    private val scope: CoroutineScope,
    private val clearLocal: suspend () -> Unit
) {
    private val registrations = Collections.synchronizedList(mutableListOf<ListenerRegistration>())
    private val applyLock = Mutex()

    // Starts syncing whenever someone signs in and stops, wiping local data, when they sign out.
    fun follow(userIds: Flow<String?>) {
        scope.launch(Dispatchers.IO) {
            var previous: String? = null
            userIds.distinctUntilChanged().collectLatest { uid ->
                stop()
                if (previous != null && uid != previous) clearLocal()
                previous = uid
                if (uid != null) start()
            }
        }
    }

    // Tidies up local cars, uploads anything made offline, then listens for cloud changes.
    private suspend fun start() {
        reconcile()
        uploadLocal()
        val cars = cloud.cars() ?: return
        registrations += cars.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Cars listener error", error)
                return@addSnapshotListener
            }
            snapshot?.let { apply(it) { change -> applyCarChange(change) } }
        }
    }

    // Removes every cloud listener.
    private fun stop() {
        synchronized(registrations) {
            registrations.forEach(ListenerRegistration::remove)
            registrations.clear()
        }
    }

    // Applies a snapshot's changes one document at a time so one bad document can't drop the rest.
    private fun apply(snapshot: QuerySnapshot, handle: suspend (DocumentChange) -> Unit) {
        scope.launch(Dispatchers.IO) {
            applyLock.withLock {
                snapshot.documentChanges.forEach { change ->
                    try {
                        handle(change)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Skipping document ${change.document.reference.path}", e)
                    }
                }
            }
        }
    }

    // Mirrors one car document change into the database.
    private suspend fun applyCarChange(change: DocumentChange) {
        when (change.type) {
            DocumentChange.Type.ADDED -> {
                val carId = saveCar(change.document) ?: return
                listenToRecords(change.document.id, carId)
            }

            DocumentChange.Type.MODIFIED -> saveCar(change.document)

            DocumentChange.Type.REMOVED ->
                carDao.getCarByFirestoreId(change.document.id)?.let { carDao.markDeleted(it.id) }
        }
    }

    // Inserts or updates a car from its document, keeping a fresher local odometer, and returns its id.
    private suspend fun saveCar(doc: DocumentSnapshot): Long? {
        val dto = doc.toCarDto() ?: return null
        val existing = carDao.getCarByFirestoreId(doc.id) ?: return carDao.insert(dto.toEntity(doc.id))

        val remoteSyncedAt = dto.lastSyncedAt ?: 0L
        val localSyncedAt = existing.lastSyncedAt?.toEpochMilli() ?: 0L
        val remoteIsFresher = remoteSyncedAt >= localSyncedAt
        carDao.update(
            existing.copy(
                name = dto.name,
                currentMileage = if (remoteIsFresher) dto.currentMileage else existing.currentMileage,
                smartcarId = dto.smartcarId ?: existing.smartcarId,
                isDeleted = dto.isDeleted,
                lastSyncedAt = if (remoteIsFresher) {
                    dto.lastSyncedAt?.let(Instant::ofEpochMilli) ?: existing.lastSyncedAt
                } else {
                    existing.lastSyncedAt
                },
                lastBackgroundCheckAt = dto.lastBackgroundCheckAt?.let(Instant::ofEpochMilli)
                    ?: existing.lastBackgroundCheckAt
            )
        )
        return existing.id
    }

    // Listens to a car's service, fill-up and odometer subcollections.
    private fun listenToRecords(carFirestoreId: String, carId: Long) {
        listen(carFirestoreId, CloudPaths.MAINTENANCE_RECORDS) { change ->
            val doc = change.document
            val existing = maintenanceDao.getRecordByFirestoreId(doc.id)
            if (change.type == DocumentChange.Type.REMOVED) {
                existing?.let { maintenanceDao.delete(it) }
                return@listen
            }
            val entity = doc.toMaintenanceRecordDto()?.toEntity(existing?.id ?: 0, carId, doc.id) ?: return@listen
            if (existing == null) maintenanceDao.insert(entity) else maintenanceDao.update(entity)
        }

        listen(carFirestoreId, CloudPaths.GAS_RECORDS) { change ->
            val doc = change.document
            val existing = gasDao.getRecordByFirestoreId(doc.id)
            if (change.type == DocumentChange.Type.REMOVED) {
                existing?.let { gasDao.delete(it) }
                return@listen
            }
            val entity = doc.toGasRecordDto()?.toEntity(existing?.id ?: 0, carId, doc.id) ?: return@listen
            if (existing == null) gasDao.insert(entity) else gasDao.update(entity)
        }

        listen(carFirestoreId, CloudPaths.ODOMETER_READINGS) { change ->
            val doc = change.document
            val existing = odometerDao.getReadingByFirestoreId(doc.id)
            if (change.type == DocumentChange.Type.REMOVED) {
                existing?.let { odometerDao.delete(it) }
                return@listen
            }
            val entity = doc.toOdometerReadingDto()?.toEntity(existing?.id ?: 0, carId, doc.id) ?: return@listen
            if (existing == null) odometerDao.insert(entity) else odometerDao.update(entity)
        }
    }

    // Attaches a listener to one record subcollection of a car.
    private fun listen(carFirestoreId: String, collection: String, handle: suspend (DocumentChange) -> Unit) {
        val records = cloud.records(carFirestoreId, collection) ?: return
        registrations += records.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "$collection listener error", error)
                return@addSnapshotListener
            }
            snapshot?.let { apply(it, handle) }
        }
    }

    // Soft-deletes local cars whose cloud document no longer exists.
    private suspend fun reconcile() {
        try {
            val cars = cloud.cars() ?: return
            val cloudIds = cars.get(Source.SERVER).await().documents.map { it.id }.toSet()
            carDao.getCarsOnce()
                .filter { it.firestoreCarId != null && it.firestoreCarId !in cloudIds }
                .forEach { carDao.markDeleted(it.id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed", e)
        }
    }

    // Uploads cars and records that were saved while offline, linking cars to a same-named cloud car if one exists.
    private suspend fun uploadLocal() {
        try {
            val cars = cloud.cars() ?: return
            val cloudCars = runCatching { cars.get().await().documents }.getOrDefault(emptyList())
            carDao.getCarsOnce().forEach { car ->
                val carFirestoreId = car.firestoreCarId ?: linkOrUpload(car, cloudCars)
                uploadRecords(car.id, carFirestoreId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Initial upload failed", e)
        }
    }

    // Links a local-only car to a cloud car with the same name, or uploads it as a new one.
    private suspend fun linkOrUpload(car: CarEntity, cloudCars: List<DocumentSnapshot>): String {
        val match = cloudCars.firstOrNull { it.toCarDto()?.name == car.name }
        if (match != null) {
            carDao.setFirestoreId(car.id, match.id)
            return match.id
        }
        val doc = checkNotNull(cloud.cars()).document()
        carDao.setFirestoreId(car.id, doc.id)
        doc.set(car.toDto().toFirestoreMap()).logFailure(TAG, "car upload")
        return doc.id
    }

    // Uploads a car's records that haven't reached the cloud yet.
    private suspend fun uploadRecords(carId: Long, carFirestoreId: String) {
        val maintenance = cloud.records(carFirestoreId, CloudPaths.MAINTENANCE_RECORDS) ?: return
        maintenanceDao.getRecordsOnce(carId).filter { it.firestoreId == null }.forEach { record ->
            val doc = maintenance.document()
            maintenanceDao.setFirestoreId(record.id, doc.id)
            doc.set(record.toDto().toFirestoreMap()).logFailure(TAG, "service record upload")
        }

        val gas = cloud.records(carFirestoreId, CloudPaths.GAS_RECORDS) ?: return
        gasDao.getRecordsOnce(carId).filter { it.firestoreId == null }.forEach { record ->
            val doc = gas.document()
            gasDao.setFirestoreId(record.id, doc.id)
            doc.set(record.toDto().toFirestoreMap()).logFailure(TAG, "fill-up upload")
        }

        val odometer = cloud.records(carFirestoreId, CloudPaths.ODOMETER_READINGS) ?: return
        odometerDao.getReadingsOnce(carId).filter { it.firestoreId == null }.forEach { reading ->
            val doc = odometer.document()
            odometerDao.setFirestoreId(reading.id, doc.id)
            doc.set(reading.toDto().toFirestoreMap()).logFailure(TAG, "odometer upload")
        }
    }

    private companion object {
        const val TAG = "FirestoreSync"
    }
}

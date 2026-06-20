package com.example.purincar.data.repository

import android.util.Log
import com.example.purincar.data.CarDao
import com.example.purincar.data.CarEntity
import com.example.purincar.data.GasRecord
import com.example.purincar.data.MaintenanceRecord
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "PurinCarRepository"

class PurinCarRepository(
    private val dao: CarDao,
    private val firestore: FirebaseFirestore,
    val uid: String
) {
    private val carsCol get() = firestore.collection("users").document(uid).collection("cars")
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // READS ROOM

    fun getAllCars(): Flow<List<CarEntity>> = dao.getAllCars()
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>> = dao.getRecordsForCar(carId)
    fun getRecordsForType(carId: Int, serviceType: String): Flow<List<MaintenanceRecord>> =
        dao.getRecordsForType(carId, serviceType)
    fun getGasRecordsForCar(carId: Int): Flow<List<GasRecord>> = dao.getGasRecordsForCar(carId)
    suspend fun getCarBySmartcarId(smartcarId: String): CarEntity? = dao.getCarBySmartcarId(smartcarId)
    suspend fun getCarById(id: Int): CarEntity? = dao.getCarById(id)

    // CAR WRITES

    suspend fun insertCar(car: CarEntity): Long {
        val roomId = dao.insertCar(car)
        val carWithRoomId = car.copy(id = roomId.toInt())
        val fsDoc = carsCol.document()
        val carWithFsId = carWithRoomId.copy(firestoreCarId = fsDoc.id)
        // Persist firestoreCarId only after Firestore acks; otherwise reconcile
        // can race and soft-delete this row before the write lands.
        try {
            fsDoc.set(carWithFsId.toFirestoreMap()).await()
            dao.updateCar(carWithFsId)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore insert failed; will retry via syncLocalDataToCloud", e)
        }
        return roomId
    }

    suspend fun updateCar(car: CarEntity) {
        dao.updateCar(car)
        car.firestoreCarId?.let { fsId ->
            carsCol.document(fsId).set(car.toFirestoreMap())
        }
    }

    suspend fun deleteCar(car: CarEntity) {
        val softDeleted = car.copy(isDeleted = true)
        dao.updateCar(softDeleted)
        car.firestoreCarId?.let { fsId ->
            // Soft-delete on the server too so the snapshot listener on other
            // devices receives MODIFIED (with isDeleted=true) instead of REMOVED,
            // which would trigger a destructive Room delete + cascade.
            carsCol.document(fsId).update("isDeleted", true)
        }
    }

    // WRITES MAINTENANCE RECORDS

    suspend fun insertRecord(record: MaintenanceRecord): Long {
        val roomId = dao.insertRecord(record)
        val withRoomId = record.copy(id = roomId.toInt())
        val car = dao.getCarById(record.carId) ?: return roomId
        val carFsId = car.firestoreCarId ?: return roomId
        val fsDoc = carsCol.document(carFsId).collection("maintenance_records").document()
        dao.updateRecord(withRoomId.copy(firestoreId = fsDoc.id))
        fsDoc.set(withRoomId.copy(firestoreId = fsDoc.id).toFirestoreMap())
        return roomId
    }

    suspend fun updateRecord(record: MaintenanceRecord) {
        dao.updateRecord(record)
        val fsId = record.firestoreId ?: return
        val car = dao.getCarById(record.carId) ?: return
        val carFsId = car.firestoreCarId ?: return
        carsCol.document(carFsId).collection("maintenance_records").document(fsId)
            .set(record.toFirestoreMap())
    }

    suspend fun deleteRecord(record: MaintenanceRecord) {
        dao.deleteRecord(record)
        val fsId = record.firestoreId ?: return
        val car = dao.getCarById(record.carId) ?: return
        val carFsId = car.firestoreCarId ?: return
        carsCol.document(carFsId).collection("maintenance_records").document(fsId).delete()
    }

    // WRITES GAS RECORDS

    suspend fun insertGasRecord(record: GasRecord): Long {
        val roomId = dao.insertGasRecord(record)
        val withRoomId = record.copy(id = roomId.toInt())
        val car = dao.getCarById(record.carId) ?: return roomId
        val carFsId = car.firestoreCarId ?: return roomId
        val fsDoc = carsCol.document(carFsId).collection("gas_records").document()
        dao.updateGasRecord(withRoomId.copy(firestoreId = fsDoc.id))
        fsDoc.set(withRoomId.copy(firestoreId = fsDoc.id).toFirestoreMap())
        return roomId
    }

    suspend fun updateGasRecord(record: GasRecord) {
        dao.updateGasRecord(record)
        val fsId = record.firestoreId ?: return
        val car = dao.getCarById(record.carId) ?: return
        val carFsId = car.firestoreCarId ?: return
        carsCol.document(carFsId).collection("gas_records").document(fsId)
            .set(record.toFirestoreMap())
    }

    suspend fun deleteGasRecord(record: GasRecord) {
        dao.deleteGasRecord(record)
        val fsId = record.firestoreId ?: return
        val car = dao.getCarById(record.carId) ?: return
        val carFsId = car.firestoreCarId ?: return
        carsCol.document(carFsId).collection("gas_records").document(fsId).delete()
    }

    // CLOUD LISTENERS
    fun startListening(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            reconcileWithFirestore()
            syncLocalDataToCloud()

            val reg = carsCol.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Cars listener error", error)
                    return@addSnapshotListener
                }
                snapshot ?: return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED -> {
                                val carRoomId = syncCarFromFirestore(change.document)
                                if (carRoomId != null) {
                                    setupSubcollectionListeners(
                                        carFsId = change.document.id,
                                        carRoomId = carRoomId,
                                        scope = scope
                                    )
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                syncCarFromFirestore(change.document)
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Only fires if the doc was hard-deleted (e.g. from the
                                // Firestore console). Treat as a soft-delete locally to
                                // honor the "soft deletes only" invariant.
                                val existing = dao.getCarByFirestoreId(change.document.id)
                                existing?.let { dao.updateCar(it.copy(isDeleted = true)) }
                            }
                        }
                    }
                }
            }
            listenerRegistrations.add(reg)
        }
    }
    fun stopListening() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    // HELPERS
    private suspend fun syncCarFromFirestore(doc: DocumentSnapshot): Int? {
        val name = doc.getString("name") ?: return null
        val currentMileage = (doc.getLong("currentMileage") ?: 0L).toInt()
        val smartcarId = doc.getString("smartcarId")
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val lastSyncedAt = doc.getLong("lastSyncedAt")
        val lastBackgroundCheckAt = doc.getLong("lastBackgroundCheckAt")

        val existing = dao.getCarByFirestoreId(doc.id)
        return if (existing == null) {
            val roomId = dao.insertCar(
                CarEntity(
                    name = name,
                    currentMileage = currentMileage,
                    smartcarId = smartcarId,
                    firestoreCarId = doc.id,
                    isDeleted = isDeleted,
                    lastSyncedAt = lastSyncedAt,
                    lastBackgroundCheckAt = lastBackgroundCheckAt
                )
            )
            roomId.toInt()
        } else {
            dao.updateCar(
                existing.copy(
                    name = name,
                    currentMileage = currentMileage,
                    smartcarId = smartcarId ?: existing.smartcarId,
                    isDeleted = isDeleted,
                    lastSyncedAt = lastSyncedAt ?: existing.lastSyncedAt,
                    lastBackgroundCheckAt = lastBackgroundCheckAt ?: existing.lastBackgroundCheckAt
                )
            )
            existing.id
        }
    }

    private fun setupSubcollectionListeners(carFsId: String, carRoomId: Int, scope: CoroutineScope) {
        val maintenanceReg = carsCol.document(carFsId).collection("maintenance_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val doc = change.document
                                val fsId = doc.id
                                val existing = dao.getRecordByFirestoreId(fsId)
                                val record = MaintenanceRecord(
                                    id = existing?.id ?: 0,
                                    carId = carRoomId,
                                    serviceType = doc.getString("serviceType") ?: return@launch,
                                    date = doc.getString("date") ?: return@launch,
                                    mileageAtService = (doc.getLong("mileageAtService") ?: 0L).toInt(),
                                    description = doc.getString("description") ?: "",
                                    cost = doc.getDouble("cost") ?: 0.0,
                                    firestoreId = fsId
                                )
                                if (existing == null) dao.insertRecord(record)
                                else dao.updateRecord(record)
                            }
                            DocumentChange.Type.REMOVED -> {
                                val existing = dao.getRecordByFirestoreId(change.document.id)
                                existing?.let { dao.deleteRecord(it) }
                            }
                        }
                    }
                }
            }

        val gasReg = carsCol.document(carFsId).collection("gas_records")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val doc = change.document
                                val fsId = doc.id
                                val existing = dao.getGasRecordByFirestoreId(fsId)
                                val record = GasRecord(
                                    id = existing?.id ?: 0,
                                    carId = carRoomId,
                                    date = doc.getString("date") ?: return@launch,
                                    gallons = doc.getDouble("gallons") ?: 0.0,
                                    totalCost = doc.getDouble("totalCost") ?: 0.0,
                                    notes = doc.getString("notes") ?: "",
                                    firestoreId = fsId
                                )
                                if (existing == null) dao.insertGasRecord(record)
                                else dao.updateGasRecord(record)
                            }
                            DocumentChange.Type.REMOVED -> {
                                val existing = dao.getGasRecordByFirestoreId(change.document.id)
                                existing?.let { dao.deleteGasRecord(it) }
                            }
                        }
                    }
                }
            }

        listenerRegistrations.add(maintenanceReg)
        listenerRegistrations.add(gasReg)
    }

    private suspend fun reconcileWithFirestore() {
        try {
            val fsIds = carsCol.get(Source.SERVER).await().documents.map { it.id }.toSet()
            val localCars = dao.getAllCarsOnce()
            for (car in localCars) {
                val fsId = car.firestoreCarId
                if (fsId != null && fsId !in fsIds) {
                    dao.updateCar(car.copy(isDeleted = true))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed", e)
        }
    }

    private suspend fun syncLocalDataToCloud() {
        val cars = dao.getAllCarsOnce()
        // Fetch existing Firestore cars once to avoid creating duplicates for the same car
        val existingFsDocs = try { carsCol.get().await().documents } catch (e: Exception) { emptyList() }
        for (car in cars) {
            val carFsId = if (car.firestoreCarId == null) {
                // Check if Firestore already has a car with the same name (e.g. from another device)
                val matchingDoc = existingFsDocs.firstOrNull { it.getString("name") == car.name }
                if (matchingDoc != null) {
                    // Link local car to the existing Firestore doc instead of creating a duplicate
                    dao.updateCar(car.copy(firestoreCarId = matchingDoc.id))
                    matchingDoc.id
                } else {
                    val fsDoc = carsCol.document()
                    dao.updateCar(car.copy(firestoreCarId = fsDoc.id))
                    fsDoc.set(car.copy(firestoreCarId = fsDoc.id).toFirestoreMap())
                    fsDoc.id
                }
            } else {
                car.firestoreCarId
            }

            val records = dao.getRecordsForCarOnce(car.id)
            for (record in records) {
                if (record.firestoreId == null) {
                    val fsDoc = carsCol.document(carFsId)
                        .collection("maintenance_records").document()
                    dao.updateRecord(record.copy(firestoreId = fsDoc.id))
                    fsDoc.set(record.copy(firestoreId = fsDoc.id).toFirestoreMap())
                }
            }

            val gasRecords = dao.getGasRecordsForCarOnce(car.id)
            for (record in gasRecords) {
                if (record.firestoreId == null) {
                    val fsDoc = carsCol.document(carFsId)
                        .collection("gas_records").document()
                    dao.updateGasRecord(record.copy(firestoreId = fsDoc.id))
                    fsDoc.set(record.copy(firestoreId = fsDoc.id).toFirestoreMap())
                }
            }
        }
    }
}

// FIREBASE SERIALIZATION

private fun CarEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "currentMileage" to currentMileage,
    "smartcarId" to smartcarId,
    "isDeleted" to isDeleted,
    "lastSyncedAt" to lastSyncedAt,
    "lastBackgroundCheckAt" to lastBackgroundCheckAt
)

private fun MaintenanceRecord.toFirestoreMap(): Map<String, Any?> = mapOf(
    "serviceType" to serviceType,
    "date" to date,
    "mileageAtService" to mileageAtService,
    "description" to description,
    "cost" to cost
)

private fun GasRecord.toFirestoreMap(): Map<String, Any?> = mapOf(
    "date" to date,
    "gallons" to gallons,
    "totalCost" to totalCost,
    "notes" to notes
)

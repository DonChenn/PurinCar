// Where each signed-in user's cars and records live in Firestore.
package com.example.purincar.data.sync

import android.util.Log
import com.example.purincar.data.car.CarDao
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class CloudPaths(
    private val firestore: FirebaseFirestore,
    private val carDao: CarDao,
    private val uid: () -> String?
) {
    // The signed-in user's cars collection, or null when nobody is signed in.
    fun cars(): CollectionReference? =
        uid()?.let { firestore.collection(USERS).document(it).collection(CARS) }

    // A record subcollection under a car's cloud document, or null if the car isn't in the cloud yet.
    fun records(carFirestoreId: String, collection: String): CollectionReference? =
        cars()?.document(carFirestoreId)?.collection(collection)

    // A record subcollection for a local car, looked up by its Room id.
    suspend fun recordsFor(carId: Long, collection: String): CollectionReference? {
        val carFirestoreId = carDao.getCarOnce(carId)?.firestoreCarId ?: return null
        return records(carFirestoreId, collection)
    }

    companion object {
        const val USERS = "users"
        const val CARS = "cars"
        const val MAINTENANCE_RECORDS = "maintenance_records"
        const val GAS_RECORDS = "gas_records"
        const val ODOMETER_READINGS = "odometer_readings"
    }
}

// Lets a Firestore write finish in the background, logging it if the server rejects it.
fun <T> Task<T>.logFailure(tag: String, what: String): Task<T> =
    addOnFailureListener { Log.e(tag, "Firestore $what failed", it) }

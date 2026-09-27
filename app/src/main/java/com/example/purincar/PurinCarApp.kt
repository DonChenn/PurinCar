// App startup: creates the shared database, cloud, Smartcar and repository objects every screen uses.
package com.example.purincar

import android.app.Application
import com.example.purincar.core.network.NetworkModule
import com.example.purincar.core.network.SmartcarApi
import com.example.purincar.data.auth.AuthRepository
import com.example.purincar.data.car.CarRepository
import com.example.purincar.data.gas.GasRepository
import com.example.purincar.data.local.PurinCarDatabase
import com.example.purincar.data.maintenance.MaintenanceRepository
import com.example.purincar.data.odometer.OdometerRepository
import com.example.purincar.data.smartcar.SmartcarRepository
import com.example.purincar.data.smartcar.SmartcarTokenStore
import com.example.purincar.data.sync.CloudPaths
import com.example.purincar.data.sync.FirestoreSync
import com.example.purincar.data.work.MaintenanceCheckWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(app: Application) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = PurinCarDatabase.create(app)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val smartcarApi: SmartcarApi = NetworkModule.createSmartcarApi()

    val auth = AuthRepository(app, firebaseAuth, appScope)
    private val cloud = CloudPaths(FirebaseFirestore.getInstance(), database.carDao(), auth::currentUserId)

    val cars = CarRepository(database.carDao(), cloud)
    val maintenance = MaintenanceRepository(database.maintenanceDao(), cloud)
    val gas = GasRepository(database.gasDao(), cloud)
    val odometer = OdometerRepository(database.odometerDao(), cloud)
    val smartcar = SmartcarRepository(smartcarApi, SmartcarTokenStore(app), cars, odometer, appScope)

    private val sync = FirestoreSync(
        cloud = cloud,
        carDao = database.carDao(),
        maintenanceDao = database.maintenanceDao(),
        gasDao = database.gasDao(),
        odometerDao = database.odometerDao(),
        scope = appScope,
        clearLocal = database::clearAllTables
    ).apply { follow(auth.userId) }
}

class PurinCarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        MaintenanceCheckWorker.schedule(this)
    }
}

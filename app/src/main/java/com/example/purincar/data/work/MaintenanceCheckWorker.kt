// Daily background job that refreshes the odometer from Smartcar and warns when a service is coming due.
package com.example.purincar.data.work

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.purincar.PurinCarApp
import com.example.purincar.core.common.AppConfig
import com.example.purincar.data.maintenance.serviceStatuses
import java.time.Instant
import java.util.concurrent.TimeUnit

class MaintenanceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PurinCarApp).container
        container.smartcar.syncVehicle(createIfMissing = false)

        val notifier = MaintenanceNotifier(applicationContext)
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cars = container.cars.carsOnce()

        cars.forEach { car ->
            val records = container.maintenance.recordsOnce(car.id)
            serviceStatuses(car.currentMileage, records)
                .filter { it.mileageProgress != null || it.timeProgress != null }
                .forEach { status ->
                    val key = "${car.id}_${status.type.label}"
                    val lastNotified = prefs.getFloat(key, 0f)
                    val crossed = Thresholds.filter { it <= status.progress && it > lastNotified }.maxOrNull()
                    if (crossed != null) {
                        notifier.notifyDue(car.name, status.type, crossed, status.progress)
                        prefs.edit { putFloat(key, crossed) }
                    }
                    if (status.progress < ResetBelow && lastNotified > 0f) {
                        prefs.edit { remove(key) }
                    }
                }
        }

        val checkedAt = Instant.now()
        cars.forEach { container.cars.markBackgroundCheck(it.id, checkedAt) }
        return Result.success()
    }

    companion object {
        private const val PREFS_NAME = "maintenance_notifications"
        private val Thresholds = listOf(0.50f, 0.75f, 0.90f)
        private const val ResetBelow = 0.10f

        // Schedules the check to run once a day whenever the phone is online, keeping any existing schedule.
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AppConfig.MAINTENANCE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

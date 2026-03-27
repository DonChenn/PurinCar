package com.example.purincar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purincar.data.CarDao
import com.example.purincar.data.PurinCarDatabase
import kotlinx.coroutines.flow.first
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MaintenanceCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val client = OkHttpClient()
    private val clientId = BuildConfig.SMARTCAR_CLIENT_ID
    private val clientSecret = BuildConfig.SMARTCAR_CLIENT_SECRET

    private val mileageIntervals = mapOf(
        "Engine Oil" to 5000, "Air Filters" to 15000, "Engine Coolant" to 30000,
        "Brake Fluid" to 30000, "Battery Fan" to 30000, "Transmission Fluid" to 60000,
        "Spark Plugs" to 100000
    )
    private val timeIntervals = mapOf(
        "Engine Oil" to 180, "Air Filters" to 365, "Engine Coolant" to 730,
        "Brake Fluid" to 730, "Battery Fan" to 1095, "Transmission Fluid" to 1460,
        "Spark Plugs" to 1825
    )

    private val thresholds = listOf(0.50f, 0.75f, 0.90f)
    private val thresholdLabels = mapOf(0.50f to "50%", 0.75f to "75%", 0.90f to "90%")

    private val notificationManager by lazy {
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also {
            it.createNotificationChannel(
                NotificationChannel("maintenance", "Maintenance Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    override suspend fun doWork(): Result {
        val dao = PurinCarDatabase.getDatabase(applicationContext).carDao()

        // Sync mileage from Smartcar before checking thresholds
        syncSmartcar(dao)

        val cars = dao.getAllCars().first()
        val today = LocalDate.now()
        val prefs = applicationContext.getSharedPreferences("maintenance_notifications", Context.MODE_PRIVATE)

        for (car in cars) {
            val records = dao.getRecordsForCar(car.id).first()

            for ((type, mInterval) in mileageIntervals) {
                val last = records.filter { it.serviceType == type }.maxByOrNull { it.mileageAtService }
                val lastMileage = last?.mileageAtService ?: 0

                val mDriven = (car.currentMileage - lastMileage).coerceAtLeast(0)
                val mProgress = mDriven.toFloat() / mInterval

                val tInterval = timeIntervals[type] ?: continue
                val daysElapsed = last?.date?.let {
                    try { ChronoUnit.DAYS.between(LocalDate.parse(it), today).toInt() } catch (e: Exception) { 0 }
                } ?: 0
                val tProgress = daysElapsed.toFloat() / tInterval

                val progress = maxOf(mProgress, tProgress)

                val prefKey = "${car.id}_$type"
                val lastNotifiedThreshold = prefs.getFloat(prefKey, 0f)

                val newThreshold = thresholds.filter { it <= progress && it > lastNotifiedThreshold }.maxOrNull()
                if (newThreshold != null) {
                    val label = thresholdLabels[newThreshold] ?: continue
                    postNotification(car.name, type, label, progress)
                    prefs.edit { putFloat(prefKey, newThreshold) }
                }

                if (progress < 0.10f && lastNotifiedThreshold > 0f) {
                    prefs.edit { remove(prefKey) }
                }
            }
        }

        return Result.success()
    }

    private suspend fun syncSmartcar(dao: CarDao) {
        try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val securePrefs = EncryptedSharedPreferences.create(
                applicationContext, "smartcar_prefs", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            var accessToken = securePrefs.getString("access_token", null) ?: return
            val refreshToken = securePrefs.getString("refresh_token", null)

            // Check token validity, refresh if needed
            val checkRes = client.newCall(
                Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            ).execute()

            if (checkRes.code == 401 && refreshToken != null) {
                checkRes.body?.close()
                val refreshRes = client.newCall(
                    Request.Builder()
                        .url("https://auth.smartcar.com/oauth/token")
                        .header("Authorization", Credentials.basic(clientId, clientSecret))
                        .post(
                            FormBody.Builder()
                                .add("grant_type", "refresh_token")
                                .add("refresh_token", refreshToken)
                                .build()
                        )
                        .build()
                ).execute()

                if (!refreshRes.isSuccessful) return
                val refreshBody = refreshRes.body?.string() ?: return
                val newJson = JSONObject(refreshBody)
                accessToken = newJson.getString("access_token")
                val newRefresh = newJson.optString("refresh_token", refreshToken)
                securePrefs.edit {
                    putString("access_token", accessToken)
                    putString("refresh_token", newRefresh)
                }
            } else if (!checkRes.isSuccessful) {
                checkRes.body?.close()
                return
            } else {
                checkRes.body?.close()
            }

            // Fetch vehicles list (always use a fresh request so we have a valid response body)
            val vehiclesRes = client.newCall(
                Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            ).execute()
            if (!vehiclesRes.isSuccessful) return
            val vehiclesBody = vehiclesRes.body?.string() ?: return
            val vehicleId = JSONObject(vehiclesBody).getJSONArray("vehicles").getString(0)

            // Fetch odometer
            val odoRes = client.newCall(
                Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId/odometer")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            ).execute()
            val odoBody = odoRes.body?.string() ?: return
            val miles = (JSONObject(odoBody).optDouble("distance", 0.0) * 0.621371).toInt()

            // Update the car's mileage in the database
            val existingCar = dao.getCarBySmartcarId(vehicleId)
            if (existingCar != null && miles > 0) {
                dao.updateCar(existingCar.copy(currentMileage = miles, lastSyncedAt = System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            // Sync failure is non-fatal — notifications will use last known mileage
        }
    }

    private fun postNotification(carName: String, serviceType: String, threshold: String, progress: Float) {
        val pct = (progress * 100).toInt()
        val notification = NotificationCompat.Builder(applicationContext, "maintenance")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$serviceType due ($threshold)")
            .setContentText("$carName — $pct% through service interval")
            .setAutoCancel(true)
            .build()
        notificationManager.notify("$carName$serviceType$threshold".hashCode(), notification)
    }
}

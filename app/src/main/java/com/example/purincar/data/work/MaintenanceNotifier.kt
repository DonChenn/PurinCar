// Posts the "service coming due" notifications.
package com.example.purincar.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.purincar.R
import com.example.purincar.data.maintenance.ServiceType
import kotlin.math.roundToInt

class MaintenanceNotifier(private val context: Context) {

    init {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_maintenance),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    // Tells the user a car's service has passed a threshold of its interval.
    fun notifyDue(carName: String, type: ServiceType, threshold: Float, progress: Float) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val service = context.getString(type.labelRes)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_due_title, service, threshold.toPercent()))
            .setContentText(context.getString(R.string.notification_due_text, carName, progress.toPercent()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify("$carName$service$threshold".hashCode(), notification)
    }

    // Turns a fraction like 0.75 into 75.
    private fun Float.toPercent(): Int = (this * 100).roundToInt()

    private companion object {
        const val CHANNEL_ID = "maintenance"
    }
}

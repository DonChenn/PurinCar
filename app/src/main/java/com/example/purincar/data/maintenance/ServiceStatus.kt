// How far each service is through its mileage and time intervals since it was last done.
package com.example.purincar.data.maintenance

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Immutable
data class ServiceStatus(
    val type: ServiceType,
    val milesDriven: Int,
    val daysElapsed: Int,
    val mileageProgress: Float?,
    val timeProgress: Float?
) {
    val progress: Float get() = maxOf(mileageProgress ?: 0f, timeProgress ?: 0f)
}

// Works out every service's status from the car's current mileage and its service history.
fun serviceStatuses(
    currentMileage: Int,
    records: List<MaintenanceRecord>,
    today: LocalDate = LocalDate.now()
): List<ServiceStatus> = ServiceType.entries.map { type ->
    val last = records.filter { it.serviceType == type }.maxByOrNull { it.mileageAtService }
    val milesDriven = (currentMileage - (last?.mileageAtService ?: 0)).coerceAtLeast(0)
    val daysElapsed = last?.let { ChronoUnit.DAYS.between(it.date, today).toInt().coerceAtLeast(0) } ?: 0
    ServiceStatus(
        type = type,
        milesDriven = milesDriven,
        daysElapsed = daysElapsed,
        mileageProgress = if (type.hasMileageInterval) milesDriven.toFloat() / type.mileageInterval else null,
        timeProgress = if (type.hasDayInterval) daysElapsed.toFloat() / type.dayInterval else null
    )
}

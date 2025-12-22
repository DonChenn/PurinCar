package com.example.purincar.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.CarDao
import com.example.purincar.data.CarEntity
import com.example.purincar.data.MaintenanceRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class ServiceStatus(
    val name: String,
    val mileageProgress: Float,
    val timeProgress: Float,
    val mileageText: String,
    val timeText: String
)

class CarDetailsViewModel(
    private val dao: CarDao,
    private val carId: Int
) : ViewModel() {

    private val serviceTypes = listOf(
        "1. Engine Oil", "2: Engine Coolant", "3: Brake Fluid",
        "4: Transmission Fluid", "5: Spark Plugs", "6: Air Filters", "7: Battery Fan"
    )

    private val mileageIntervals = mapOf(
        "1. Engine Oil" to 5000,
        "2: Engine Coolant" to 30000,
        "3: Brake Fluid" to 30000,
        "4: Transmission Fluid" to 60000,
        "5: Spark Plugs" to 100000,
        "6: Air Filters" to 15000,
        "7: Battery Fan" to 30000
    )

    private val timeIntervals = mapOf(
        "1. Engine Oil" to 180,
        "2: Engine Coolant" to 730,
        "3: Brake Fluid" to 730,
        "4: Transmission Fluid" to 1460,
        "5: Spark Plugs" to 1825,
        "6: Air Filters" to 365,
        "7: Battery Fan" to 1095
    )

    val carInfo: Flow<CarEntity?> = dao.getAllCars().map { list ->
        list.find { it.id == carId }
    }

    private val allRecords: Flow<List<MaintenanceRecord>> = dao.getRecordsForCar(carId)

    @RequiresApi(Build.VERSION_CODES.O)
    val serviceStatuses: Flow<List<ServiceStatus>> = combine(
        carInfo,
        allRecords
    ) { car, records ->
        val currentMileage = car?.currentMileage ?: 0
        val currentDate = LocalDate.now()

        serviceTypes.map { type ->
            val lastRecord = records
                .filter { it.serviceType == type }
                .maxByOrNull { it.mileageAtService }

            val lastMileage = lastRecord?.mileageAtService ?: 0
            val mInterval = mileageIntervals[type] ?: 5000
            val mDriven = (currentMileage - lastMileage).coerceAtLeast(0)
            val mProgress = (mDriven.toFloat() / mInterval.toFloat()).coerceIn(0f, 1f)

            val lastDateStr = lastRecord?.date
            val daysElapsed = if (lastDateStr != null) {
                try {
                    val lastDate = LocalDate.parse(lastDateStr)
                    ChronoUnit.DAYS.between(lastDate, currentDate).toInt().coerceAtLeast(0)
                } catch (e: DateTimeParseException) {
                    0
                }
            } else {
                0
            }
            val tInterval = timeIntervals[type] ?: 365
            val tProgress = (daysElapsed.toFloat() / tInterval.toFloat()).coerceIn(0f, 1f)

            ServiceStatus(
                name = type,
                mileageProgress = mProgress,
                timeProgress = tProgress,
                mileageText = "$mDriven / $mInterval mi",
                timeText = "$daysElapsed / $tInterval days"
            )
        }
    }

    fun updateMileage(newMileage: Int) {
        viewModelScope.launch {
            val currentCar = carInfo.firstOrNull()
            if (currentCar != null) {
                dao.updateCar(currentCar.copy(currentMileage = newMileage))
            }
        }
    }
}

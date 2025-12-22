package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.CarDao
import com.example.purincar.data.CarEntity
import com.example.purincar.data.MaintenanceRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CarDetailsViewModel(
    private val dao: CarDao,
    private val carId: Int
) : ViewModel() {

    val serviceTypes = listOf(
        "1. Engine Oil",
        "2: Engine Coolant",
        "3: Brake Fluid",
        "4: Transmission Fluid",
        "5: Spark Plugs",
        "6: Air Filters",
        "7: Battery Fan"
    )

    private val serviceIntervals = mapOf(
        "1. Engine Oil" to 5000,
        "2: Engine Coolant" to 30000,
        "3: Brake Fluid" to 30000,
        "4: Transmission Fluid" to 60000,
        "5: Spark Plugs" to 100000,
        "6: Air Filters" to 15000,
        "7: Battery Fan" to 30000
    )

    val carInfo: Flow<CarEntity?> = dao.getAllCars().map { list ->
        list.find { it.id == carId }
    }

    private val _allRecords = dao.getRecordsForCar(carId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getRecordsForType(type: String): Flow<List<MaintenanceRecord>> {
        return _allRecords.map { records ->
            records.filter { it.serviceType == type }
        }
    }

    fun getProgressForType(type: String, currentMileage: Int): Float {
        val lastRecord = _allRecords.value.filter { it.serviceType == type }
            .maxByOrNull { it.mileageAtService }

        val lastMileage = lastRecord?.mileageAtService ?: 0
        val interval = serviceIntervals[type] ?: 5000 // Default to 5000 if unknown

        val distanceDriven = currentMileage - lastMileage
        val progress = distanceDriven.toFloat() / interval.toFloat()

        return progress.coerceIn(0f, 1f)
    }

    fun updateMileage(newMileage: Int) {
        viewModelScope.launch {
            val currentCar = carInfo.firstOrNull()
            if (currentCar != null) {
                dao.updateCar(currentCar.copy(currentMileage = newMileage))
            }
        }
    }

    fun addRecord(serviceType: String, date: String, mileage: Int) {
        viewModelScope.launch {
            val newRecord = MaintenanceRecord(
                carId = carId,
                serviceType = serviceType,
                date = date,
                mileageAtService = mileage
            )
            dao.insertRecord(newRecord)
        }
    }
}

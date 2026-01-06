package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.CarDao
import com.example.purincar.data.MaintenanceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class ServiceHistoryViewModel(
    private val dao: CarDao,
    private val carId: Int,
    val serviceType: String
) : ViewModel() {

    val history: Flow<List<MaintenanceRecord>> = dao.getRecordsForType(carId, serviceType)
    val currentMileage: Flow<Int> = dao.getAllCars().map { cars ->
        cars.find { it.id == carId }?.currentMileage ?: 0
    }

    fun addRecord(date: String, mileage: Int) {
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

    fun removeRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            dao.deleteRecord(record)
        }
    }
}

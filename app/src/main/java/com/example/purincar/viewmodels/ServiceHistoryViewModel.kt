package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.data.repository.PurinCarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ServiceHistoryViewModel(
    private val repository: PurinCarRepository,
    private val carId: Int,
    val serviceType: String
) : ViewModel() {

    val history: Flow<List<MaintenanceRecord>> = repository.getRecordsForType(carId, serviceType)
    val currentMileage: Flow<Int> = repository.getAllCars().map { cars ->
        cars.find { it.id == carId }?.currentMileage ?: 0
    }

    fun addRecord(date: String, mileage: Int, cost: Double, descriptionInput: String) {
        viewModelScope.launch {
            repository.insertRecord(
                MaintenanceRecord(
                    carId = carId,
                    serviceType = serviceType,
                    date = date,
                    mileageAtService = mileage,
                    description = descriptionInput,
                    cost = cost
                )
            )
        }
    }

    fun updateRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            repository.updateRecord(record)
        }
    }

    fun removeRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }
}

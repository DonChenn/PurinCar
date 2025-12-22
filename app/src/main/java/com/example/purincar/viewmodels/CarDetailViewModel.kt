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

    val carInfo: Flow<CarEntity?> = dao.getAllCars().map { list ->
        list.find { it.id == carId }
    }

    val records: StateFlow<List<MaintenanceRecord>> = dao.getRecordsForCar(carId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecord(serviceType: String, date: String, mileage: Int) {
        viewModelScope.launch {
            val newRecord = MaintenanceRecord(
                carId = carId,
                serviceType = serviceType,
                date = date,
                mileageAtService = mileage
            )
            dao.insertRecord(newRecord)

            val currentCar = carInfo.firstOrNull()
            if (currentCar != null && mileage > currentCar.currentMileage) {

            }
        }
    }
}
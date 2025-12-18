package com.example.purincar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class CarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val carDao = database.carDao()

    val allCars: Flow<List<Car>> = carDao.getAllCars()

    // CAR OPERATIONS

    fun addCar(name: String, year: Int?, make: String?, model: String?) {
        viewModelScope.launch {
            val car = Car(
                name = name,
                year = year,
                make = make,
                model = model
            )
        }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            carDao.deleteCar(car)
        }
    }

    // RECORD OPERATIONS

    fun getRecordsForCar(carId: String): Flow<List<CarRecord>> {
        return carDao.getRecordsForCar(carId)
    }

    fun addRecord(
        carId: String,
        type: RecordType,
        date: LocalDate,
        mileage: Int,
        notes: String? = null,
        cost: Double? = null
    ) {
        viewModelScope.launch {
            val record = CarRecord(
                carId = carId,
                type = type,
                date = date,
                mileage = mileage,
                notes = notes,
            )
            carDao.insertRecord(record)
        }
    }

    fun deleteRecord(record: CarRecord) {
        viewModelScope.launch {
            carDao.deleteRecord(record)
        }
    }

    // UI STATE

    private val _selectedCar = MutableStateFlow<Car?>(null)
    val selectedCar: StateFlow<Car?> = _selectedCar.asStateFlow()

    fun selectCar(car: Car?) {
        _selectedCar.value = car
    }
}

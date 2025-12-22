package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.CarDao
import com.example.purincar.data.CarEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class CarDetails(
    val id: Int = 0,
    val name: String,
    val currentMileage: Int
)

class CarViewModel(private val dao: CarDao) : ViewModel() {
    val cars: StateFlow<List<CarDetails>> = dao.getAllCars()
        .map { entities ->
            entities.map { CarDetails(it.id, it.name, it.currentMileage) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCar(car: CarDetails) {
        if (car.name.isNotBlank()) {
            viewModelScope.launch {
                dao.insertCar(CarEntity(name = car.name, currentMileage = car.currentMileage))
            }
        }
    }

    fun removeCar(car: CarDetails) {
        viewModelScope.launch {
            dao.deleteCar(CarEntity(id = car.id, name = car.name, currentMileage = car.currentMileage))
        }
    }
}
package com.example.purincar.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CarViewModel : ViewModel() {
    var cars by mutableStateOf(listOf<CarDetails>())
        private set

    fun addCar(car: CarDetails) {
        if(car.name.isNotBlank()) {
            cars = cars + car
        }
    }

    fun removeCar(car: CarDetails) {
        cars = cars - car

    }
}
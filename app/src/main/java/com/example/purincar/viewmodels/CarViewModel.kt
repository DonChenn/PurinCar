package com.example.purincar.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CarViewModel : ViewModel() {
    var cars by mutableStateOf(listOf<String>())
        private set

    fun addCar(car: String) {
        if(car.isNotBlank()) {
            cars = cars + car
        }
    }

    fun removeCar(car: String) {
        cars = cars - car

    }
}
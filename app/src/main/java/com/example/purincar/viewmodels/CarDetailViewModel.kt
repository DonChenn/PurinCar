package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class CarDetails(
    val name: String = "",
    val mileage: Int = 0
)

class CarDetailsViewModel(
    private val car: CarDetails
) : ViewModel() {
    private val _state = MutableStateFlow(CarDetails(car.name, car.mileage))
    val state: StateFlow<CarDetails> = _state.asStateFlow()
}

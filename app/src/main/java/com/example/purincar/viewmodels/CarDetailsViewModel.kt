package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarDetails(
    val name: String = "",
    val mileage: Int = 0
)

class CarDetailsViewModel(
    private val car: String
) : ViewModel() {
    private val _state = MutableStateFlow(CarDetails(car))
    val state: StateFlow<CarDetails> = _state.asStateFlow()
}
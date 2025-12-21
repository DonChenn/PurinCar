package com.example.purincar.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.example.purincar.viewmodels.CarDetails

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object CarSelectionScreen : Route, NavKey

    @Serializable
    data class CarDetailsScreen(val car: CarDetails) : Route, NavKey
}

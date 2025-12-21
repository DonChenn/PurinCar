package com.example.purincar.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object CarSelectionScreen : Route

    @Serializable
    data class CarDetailsScreen(val car: String) : Route
}

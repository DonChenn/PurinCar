// The pages the app can navigate to.
package com.example.purincar

import androidx.navigation3.runtime.NavKey
import com.example.purincar.data.maintenance.ServiceType
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Garage : Route

    @Serializable
    data class CarDetails(val carId: Long) : Route

    @Serializable
    data class ServiceHistory(val carId: Long, val serviceType: ServiceType) : Route
}

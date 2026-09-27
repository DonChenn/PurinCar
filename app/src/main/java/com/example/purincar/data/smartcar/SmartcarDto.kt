// Smartcar responses exactly as the Smartcar API sends them.
package com.example.purincar.data.smartcar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehiclesDto(val vehicles: List<String> = emptyList())

@Serializable
data class VehicleDto(
    val id: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0
)

@Serializable
data class OdometerDto(val distance: Double = 0.0)

@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null
)

// Builds the name shown for a vehicle, like "2019 Toyota Corolla".
fun VehicleDto.displayName(): String = "$year $make $model"

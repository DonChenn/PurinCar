// Every Smartcar endpoint the app calls.
package com.example.purincar.core.network

import com.example.purincar.core.common.AppConfig
import com.example.purincar.data.smartcar.OdometerDto
import com.example.purincar.data.smartcar.TokenDto
import com.example.purincar.data.smartcar.VehicleDto
import com.example.purincar.data.smartcar.VehiclesDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SmartcarApi {
    // Lists the vehicles the signed-in Smartcar account has shared.
    @GET("vehicles")
    suspend fun getVehicles(@Header("Authorization") bearer: String): VehiclesDto

    // Loads a vehicle's make, model and year.
    @GET("vehicles/{id}")
    suspend fun getVehicle(
        @Header("Authorization") bearer: String,
        @Path("id") vehicleId: String
    ): VehicleDto

    // Loads a vehicle's odometer in kilometers.
    @GET("vehicles/{id}/odometer")
    suspend fun getOdometer(
        @Header("Authorization") bearer: String,
        @Path("id") vehicleId: String
    ): OdometerDto

    // Trades an authorization code for an access and refresh token.
    @FormUrlEncoded
    @POST(AppConfig.SMARTCAR_TOKEN_URL)
    suspend fun exchangeCode(
        @Header("Authorization") basic: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String = AppConfig.SMARTCAR_REDIRECT_URI,
        @Field("grant_type") grantType: String = "authorization_code"
    ): TokenDto

    // Trades a refresh token for a new access token.
    @FormUrlEncoded
    @POST(AppConfig.SMARTCAR_TOKEN_URL)
    suspend fun refreshToken(
        @Header("Authorization") basic: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): TokenDto
}

package com.example.purincar

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.purincar.data.CarDao
import com.example.purincar.data.CarEntity
import com.example.purincar.data.PurinCarDatabase
import com.smartcar.sdk.SmartcarAuth
import com.smartcar.sdk.SmartcarCallback
import com.smartcar.sdk.SmartcarResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private lateinit var smartcarAuth: SmartcarAuth
    private lateinit var carDao: CarDao
    private val client = OkHttpClient()

    private val CLIENT_ID = "51519156-f3db-41cf-99be-bcd5770827de"
    private val CLIENT_SECRET = BuildConfig.SMARTCAR_CLIENT_SECRET
    private val REDIRECT_URI = "sc$CLIENT_ID://exchange"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PurinCarDatabase.getDatabase(applicationContext)
        carDao = database.carDao()

        // 1. SMARTCAR AUTH
        smartcarAuth = SmartcarAuth(
            CLIENT_ID,
            REDIRECT_URI,
            arrayOf("read_odometer", "read_vehicle_info"),
            true, // Test Mode
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Toast.makeText(applicationContext, "Linking Car...", Toast.LENGTH_SHORT).show()
                        exchangeCodeForToken(code)
                    } else {
                        Log.e("Smartcar", "Auth failed: ${smartcarResponse?.toString()}")
                        Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // AUTO UPDATE
        performSilentRefresh()

        setContent {
            App(
                dao = carDao,
                onConnectSmartcar = {
                    smartcarAuth.launchAuthFlow(this)
                }
            )
        }
    }

    // -REFRESH
    private fun performSilentRefresh() {
        val prefs = getSharedPreferences("smartcar_prefs", Context.MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", null)

        if (refreshToken != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val requestBody = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build()

                    val request = Request.Builder()
                        .url("https://auth.smartcar.com/oauth/token")
                        .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val newAccessToken = json.getString("access_token")
                        val newRefreshToken = json.getString("refresh_token")

                        saveRefreshToken(newRefreshToken)

                        syncVehicleData(newAccessToken)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Auto-updated Odometer!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("Smartcar", "Refresh failed: $body")
                    }
                } catch (e: Exception) {
                    Log.e("Smartcar", "Silent refresh error", e)
                }
            }
        }
    }

    // INIT LOGIN
    private fun exchangeCodeForToken(authCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", authCode)
                    .add("redirect_uri", REDIRECT_URI)
                    .build()

                val request = Request.Builder()
                    .url("https://auth.smartcar.com/oauth/token")
                    .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful || body == null) {
                    throw IOException("Token Error: $body")
                }

                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = json.getString("refresh_token")

                saveRefreshToken(refreshToken)

                syncVehicleData(accessToken)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SHARED DATA FETCH
    private suspend fun syncVehicleData(accessToken: String) {
        try {
            // ID
            val vehiclesReq = Request.Builder()
                .url("https://api.smartcar.com/v2.0/vehicles")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val vehiclesRes = client.newCall(vehiclesReq).execute()
            val vehicleId = JSONObject(vehiclesRes.body!!.string()).getJSONArray("vehicles").getString(0)

            // ATTRIBUTES
            val attrReq = Request.Builder()
                .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val attrJson = JSONObject(client.newCall(attrReq).execute().body!!.string())
            val carName = "${attrJson.getInt("year")} ${attrJson.getString("make")} ${attrJson.getString("model")}"

            // ODOMETER
            val odoReq = Request.Builder()
                .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId/odometer")
                .header("Authorization", "Bearer $accessToken")
                .build()
            val odoJson = JSONObject(client.newCall(odoReq).execute().body!!.string())
            val miles = (odoJson.getDouble("distance") * 0.621371).toInt()

            // DATABASE
            val existingCar = carDao.getCarBySmartcarId(vehicleId)

            if (existingCar != null) {
                // UPDATE
                val updatedCar = existingCar.copy(currentMileage = miles, name = carName)
                carDao.updateCar(updatedCar)
                Log.d("Smartcar", "Updated car: $carName to $miles miles")
            } else {
                // INSERT
                val newCar = CarEntity(
                    name = carName,
                    currentMileage = miles,
                    smartcarId = vehicleId
                )
                carDao.insertCar(newCar)
                Log.d("Smartcar", "Inserted new car: $carName")
            }

        } catch (e: Exception) {
            Log.e("Smartcar", "Sync Error", e)
            throw e
        }
    }

    private fun saveRefreshToken(token: String) {
        val prefs = getSharedPreferences("smartcar_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("refresh_token", token) }
    }
}

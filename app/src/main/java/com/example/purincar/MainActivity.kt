package com.example.purincar

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

        smartcarAuth = SmartcarAuth(
            CLIENT_ID,
            REDIRECT_URI,
            arrayOf(
                "read_vehicle_info",
                "read_fuel",
                "read_odometer",
                "read_security"
            ),
            false, // test
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Toast.makeText(
                            applicationContext,
                            "Fetching Vehicle Data...",
                            Toast.LENGTH_SHORT
                        ).show()
                        exchangeCodeForToken(code)
                    } else {
                        Log.e("Smartcar", "Auth failed: ${smartcarResponse?.toString()}")
                        Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        )

        setContent {
            App(
                dao = carDao,
                onConnectSmartcar = {
                    smartcarAuth.launchAuthFlow(this)
                }
            )
        }

        tryAutoConnect()
    }

    /**
     Exchanges the Auth Code for Access Token
    **/
    private fun exchangeCodeForToken(authCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tokenReq = Request.Builder()
                    .url("https://auth.smartcar.com/oauth/token")
                    .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                    .post(
                        FormBody.Builder()
                            .add("grant_type", "authorization_code")
                            .add("code", authCode)
                            .add("redirect_uri", REDIRECT_URI)
                            .build()
                    )
                    .build()
                val tokenRes = client.newCall(tokenReq).execute()
                if (!tokenRes.isSuccessful) throw IOException("Token Error")

                val json = JSONObject(tokenRes.body!!.string())
                val accessToken = json.getString("access_token")
                val refreshToken = json.getString("refresh_token")

                // SAVE TOKENS
                getSharedPreferences("smartcar_prefs", MODE_PRIVATE).edit().apply {
                    putString("access_token", accessToken)
                    putString("refresh_token", refreshToken)
                    apply()
                }

                // Proceed to fetch data
                fetchCarDataWithToken(accessToken)

            } catch (e: Exception) {
                Log.e("Smartcar", "Login Error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Login Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun tryAutoConnect() {
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("smartcar_prefs", MODE_PRIVATE)
            var accessToken = prefs.getString("access_token", null)
            val refreshToken = prefs.getString("refresh_token", null)

            if (accessToken != null) {
                try {
                    // Quick check to see if token is valid
                    val req = Request.Builder()
                        .url("https://api.smartcar.com/v2.0/vehicles")
                        .header("Authorization", "Bearer $accessToken")
                        .build()

                    val res = client.newCall(req).execute()

                    if (res.code == 401 && refreshToken != null) {
                        Log.d("Smartcar", "Token expired. Refreshing...")
                        // expired token, refresh to get a new one
                        val refreshReq = Request.Builder()
                            .url("https://auth.smartcar.com/oauth/token")
                            .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                            .post(
                                FormBody.Builder()
                                    .add("grant_type", "refresh_token")
                                    .add("refresh_token", refreshToken)
                                    .build()
                            )
                            .build()

                        val refreshRes = client.newCall(refreshReq).execute()
                        if (refreshRes.isSuccessful) {
                            val newJson = JSONObject(refreshRes.body!!.string())
                            accessToken = newJson.getString("access_token")
                            val newRefresh = newJson.optString("refresh_token", refreshToken)

                            // Save new tokens
                            prefs.edit().apply {
                                putString("access_token", accessToken)
                                putString("refresh_token", newRefresh)
                                apply()
                            }

                            Log.d("Smartcar", "Token refreshed. Fetching data...")
                            fetchCarDataWithToken(accessToken!!)
                        }
                    } else if (res.isSuccessful) {
                        // Token is still good
                        Log.d("Smartcar", "Token valid. Fetching data...")
                        fetchCarDataWithToken(accessToken)
                    }
                } catch (e: Exception) {
                    Log.e("Smartcar", "Auto-connect failed", e)
                }
            }
        }
    }

    private suspend fun fetchCarDataWithToken(accessToken: String) {
        try {
            // A. GET VEHICLE ID
            val vehiclesResponse = client.newCall(
                Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            ).execute().body!!.string()

            val vehicleId = JSONObject(vehiclesResponse).getJSONArray("vehicles").getString(0)

            // B. HELPER FUNCTION FOR ENDPOINTS
            fun fetch(endpoint: String): JSONObject {
                val url = if (endpoint.isEmpty())
                    "https://api.smartcar.com/v2.0/vehicles/$vehicleId"
                else
                    "https://api.smartcar.com/v2.0/vehicles/$vehicleId/$endpoint"

                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                val res = client.newCall(req).execute()
                // Return empty JSON if call fails so app doesn't crash
                return JSONObject(res.body?.string() ?: "{}")
            }

            // C. FETCH DATA POINTS

            // Vehicle Name
            val info = fetch("")
            val carName =
                "${info.getInt("year")} ${info.getString("make")} ${info.getString("model")}"

            // Odometer
            val odoJson = fetch("odometer")
            val miles = (odoJson.optDouble("distance", 0.0) * 0.621371).toInt()

            // Fuel Level
            val fuelJson = fetch("fuel")
            Log.d("PurinCar", "Fuel JSON: $fuelJson") // Debug logging
            val fuelPercent = fuelJson.optDouble("percentRemaining", -1.0)

            // Door Locked Status
            val secJson = fetch("security")
            val isLocked = secJson.optBoolean("isLocked", false)

            // D. UPDATE DATABASE
            val existingCar = carDao.getCarBySmartcarId(vehicleId)

            val carToSave = existingCar?.copy(
                name = carName,
                currentMileage = miles,
                fuelPercent = if (fuelPercent >= 0) fuelPercent else null,
                isLocked = isLocked,
            ) ?: CarEntity(
                name = carName,
                currentMileage = miles,
                smartcarId = vehicleId,
                fuelPercent = if (fuelPercent >= 0) fuelPercent else null,
                isLocked = isLocked,
            )

            if (existingCar != null) carDao.updateCar(carToSave) else carDao.insertCar(carToSave)

            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Updated: $carName", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("Smartcar", "Sync Error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

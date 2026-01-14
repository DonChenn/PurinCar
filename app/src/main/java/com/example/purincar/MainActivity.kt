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

    // Ensure these match your actual setup
    private val CLIENT_ID = "51519156-f3db-41cf-99be-bcd5770827de"
    // Ideally use BuildConfig.SMARTCAR_CLIENT_SECRET, but for now ensure it works:
    private val CLIENT_SECRET = BuildConfig.SMARTCAR_CLIENT_SECRET
    private val REDIRECT_URI = "sc$CLIENT_ID://exchange"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DAO
        val database = PurinCarDatabase.getDatabase(applicationContext)
        carDao = database.carDao()

        smartcarAuth = SmartcarAuth(
            CLIENT_ID,
            REDIRECT_URI,
            arrayOf("read_odometer", "read_vehicle_info"),
            true,
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Toast.makeText(applicationContext, "Importing Car...", Toast.LENGTH_SHORT).show()
                        fetchCarData(code)
                    } else {
                        Log.e("Smartcar", "Auth failed: ${smartcarResponse?.toString()}")
                        Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT).show()
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
    }

    private fun fetchCarData(authCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // --- STEP 1: Exchange Auth Code for Access Token ---
                val tokenRequestBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", authCode)
                    .add("redirect_uri", REDIRECT_URI)
                    .build()

                val tokenRequest = Request.Builder()
                    .url("https://auth.smartcar.com/oauth/token")
                    .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                    .post(tokenRequestBody)
                    .build()

                val tokenResponse = client.newCall(tokenRequest).execute()
                val tokenBody = tokenResponse.body?.string()

                if (!tokenResponse.isSuccessful) {
                    throw IOException("Token Error: $tokenBody")
                }
                val accessToken = JSONObject(tokenBody!!).getString("access_token")

                // --- STEP 2: Get Vehicle ID ---
                val vehiclesReq = Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                val vehiclesRes = client.newCall(vehiclesReq).execute()
                val vehicleId = JSONObject(vehiclesRes.body!!.string())
                    .getJSONArray("vehicles").getString(0)

                // --- STEP 3: Get Vehicle Info (Name) ---
                val infoReq = Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                val infoRes = client.newCall(infoReq).execute()
                val infoJson = JSONObject(infoRes.body!!.string())

                // Create Name: "2022 TOYOTA CAMRY"
                val year = infoJson.getInt("year")
                val make = infoJson.getString("make")
                val model = infoJson.getString("model")
                val carName = "$year $make $model"

                // --- STEP 4: Get Odometer ---
                val odometerReq = Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId/odometer")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                val odoRes = client.newCall(odometerReq).execute()
                val odoJson = JSONObject(odoRes.body!!.string())

                val kilometers = odoJson.getDouble("distance")
                val miles = (kilometers * 0.621371).toInt()

                // --- STEP 5: SAVE TO DATABASE ---
                val newCar = CarEntity(
                    name = carName,
                    currentMileage = miles
                )
                carDao.insertCar(newCar) // <--- This line adds the car to your list!

                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Added: $carName", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("Smartcar", "Error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

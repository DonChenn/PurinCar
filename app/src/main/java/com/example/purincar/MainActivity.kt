// app/src/main/java/com/example/purincar/MainActivity.kt
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
import kotlin.math.roundToInt

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
                "read_odometer",
                "read_fuel",
                "read_tires",
                "read_engine_oil",
                "read_security",
                "control_security"
            ),
            true, // Test Mode
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Toast.makeText(applicationContext, "Fetching Vehicle Data...", Toast.LENGTH_SHORT).show()
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
                // ACCESS TOKEN
                val tokenReq = Request.Builder()
                    .url("https://auth.smartcar.com/oauth/token")
                    .header("Authorization", Credentials.basic(CLIENT_ID, CLIENT_SECRET))
                    .post(FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", authCode)
                        .add("redirect_uri", REDIRECT_URI)
                        .build())
                    .build()
                val tokenRes = client.newCall(tokenReq).execute()
                if (!tokenRes.isSuccessful) throw IOException("Token Error")
                val accessToken = JSONObject(tokenRes.body!!.string()).getString("access_token")

                // VEHICLE NAME
                val vehicleId = JSONObject(
                    client.newCall(
                        Request.Builder()
                            .url("https://api.smartcar.com/v2.0/vehicles")
                            .header("Authorization", "Bearer $accessToken")
                            .build()
                    ).execute().body!!.string()
                ).getJSONArray("vehicles").getString(0)

                // JSON ENDPOINT
                fun fetch(endpoint: String): JSONObject {
                    val req = Request.Builder()
                        .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId/$endpoint")
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val res = client.newCall(req).execute()
                    return JSONObject(res.body?.string() ?: "{}")
                }

                // FETCH ALL POINTS

                // Info
                val info = fetch("")
                val carName = "${info.getInt("year")} ${info.getString("make")} ${info.getString("model")}"

                // Odometer
                val odoJson = fetch("odometer")
                val miles = (odoJson.optDouble("distance", 0.0) * 0.621371).toInt()

                // Fuel & Range
                val fuelJson = fetch("fuel")
                val fuelPercent = fuelJson.optDouble("percentRemaining", -1.0)
                val range = (fuelJson.optDouble("range", 0.0) * 0.621371) // Convert km to miles

                // Oil Life
                val oilJson = fetch("engine/oil")
                val oilLife = oilJson.optDouble("lifeRemaining", -1.0)

                // Security (Doors)
                val secJson = fetch("security")
                val isLocked = secJson.optBoolean("isLocked", false)

                // Tires
                val tiresJson = fetch("tires")
                val fl = tiresJson.optDouble("frontLeft", 0.0).roundToInt()
                val fr = tiresJson.optDouble("frontRight", 0.0).roundToInt()
                val rl = tiresJson.optDouble("backLeft", 0.0).roundToInt()
                val rr = tiresJson.optDouble("backRight", 0.0).roundToInt()
                val tireString = "FL:$fl FR:$fr RL:$rl RR:$rr"

                // UPDATE DATABASE
                val existingCar = carDao.getCarBySmartcarId(vehicleId)

                val carToSave = existingCar?.copy(
                    name = carName,
                    currentMileage = miles,
                    fuelPercent = if(fuelPercent >= 0) fuelPercent else null,
                    range = range,
                    oilLife = if(oilLife >= 0) oilLife else null,
                    tirePressure = tireString,
                    isLocked = isLocked
                )
                    ?: CarEntity(
                        name = carName,
                        currentMileage = miles,
                        smartcarId = vehicleId,
                        fuelPercent = if(fuelPercent >= 0) fuelPercent else null,
                        range = range,
                        oilLife = if(oilLife >= 0) oilLife else null,
                        tirePressure = tireString,
                        isLocked = isLocked
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
}

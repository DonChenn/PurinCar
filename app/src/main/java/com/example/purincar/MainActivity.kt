package com.example.purincar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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
    private val client = OkHttpClient()
    private val CLIENT_ID = "51519156-f3db-41cf-99be-bcd5770827de"
    private val CLIENT_SECRET = BuildConfig.SMARTCAR_CLIENT_SECRET
    private val REDIRECT_URI = "sc$CLIENT_ID://exchange"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        smartcarAuth = SmartcarAuth(
            CLIENT_ID,
            REDIRECT_URI,
            arrayOf("read_odometer", "read_vehicle_info"),
            true, // TRUE = Test Mode (Simulated Car). Set to FALSE for real Toyota login.
            object : SmartcarCallback {
                override fun handleResponse(response: SmartcarResponse?) {
                    val code = response?.code
                    if (code != null) {
                        Log.d("Smartcar", "Got Auth Code. Fetching data...")
                        fetchCarData(code)
                    } else {
                        Log.e("Smartcar", "Auth failed: ${response?.description}")
                        Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        val database = PurinCarDatabase.getDatabase(applicationContext)
        val carDao = database.carDao()

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
                // 1. Exchange Auth Code for Access Token
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

                // 2. Get the first Vehicle ID
                val vehiclesRequest = Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                val vehiclesResponse = client.newCall(vehiclesRequest).execute()
                val vehiclesJson = JSONObject(vehiclesResponse.body!!.string())
                val vehicleId = vehiclesJson.getJSONArray("vehicles").getString(0)

                // 3. Get Odometer
                val odometerRequest = Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles/$vehicleId/odometer")
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                val odometerResponse = client.newCall(odometerRequest).execute()
                val odometerJson = JSONObject(odometerResponse.body!!.string())

                // Smartcar returns distance in Kilometers. Convert to Miles if needed.
                val kilometers = odometerJson.getDouble("distance")
                val miles = (kilometers * 0.621371).toInt()

                // 4. Update UI
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Success! Odometer: $miles miles", Toast.LENGTH_LONG).show()
                    Log.d("Smartcar", "FINAL ODOMETER: $miles")

                    // TODO: Here you can save 'miles' to your database using carDao
                }

            } catch (e: Exception) {
                Log.e("Smartcar", "Error fetching data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

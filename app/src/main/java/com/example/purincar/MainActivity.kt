package com.example.purincar

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.purincar.data.PurinCarDatabase
import com.smartcar.sdk.SmartcarAuth
import com.smartcar.sdk.SmartcarCallback
import com.smartcar.sdk.SmartcarResponse

class MainActivity : ComponentActivity() {

    private lateinit var smartcarAuth: SmartcarAuth

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        smartcarAuth = SmartcarAuth(
            "YOUR_CLIENT_ID",
            "scYOUR_CLIENT_ID://exchange",
            arrayOf("read_odometer", "read_vehicle_info"),
            true, // Test mode (set to false for real cars)
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    // 3. Handle the Auth Code
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Log.d("Smartcar", "Auth Code: $code")
                        // TODO: Send this 'code' to your backend to exchange for an access token
                        // Example: viewModel.exchangeCodeForToken(code)
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
}

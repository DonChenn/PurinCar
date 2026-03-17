package com.example.purincar

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purincar.data.CarEntity
import com.example.purincar.data.PurinCarDatabase
import com.example.purincar.data.repository.PurinCarRepository
import com.example.purincar.screens.SignInScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var smartcarAuth: SmartcarAuth
    private val client = OkHttpClient()

    private val auth = FirebaseAuth.getInstance()
    private var currentUser by mutableStateOf<FirebaseUser?>(auth.currentUser)
    private var repository: PurinCarRepository? = null

    private val CLIENT_ID = BuildConfig.SMARTCAR_CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.SMARTCAR_CLIENT_SECRET
    private val REDIRECT_URI = "sc$CLIENT_ID://exchange"

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    // GOOGLE SIGN IN
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign in failed", e)
            Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PurinCarDatabase.getDatabase(applicationContext)
        val dao = database.carDao()

        // If already signed in, create the repository immediately
        auth.currentUser?.let { user ->
            repository = PurinCarRepository(dao, FirebaseFirestore.getInstance(), user.uid).also {
                it.startListening(lifecycleScope)
            }
        }

        smartcarAuth = SmartcarAuth(
            CLIENT_ID,
            REDIRECT_URI,
            arrayOf("read_vehicle_info", "read_fuel", "read_odometer", "read_security"),
            false,
            object : SmartcarCallback {
                override fun handleResponse(smartcarResponse: SmartcarResponse?) {
                    val code = smartcarResponse?.code
                    if (code != null) {
                        Toast.makeText(applicationContext, "Fetching Vehicle Data...", Toast.LENGTH_SHORT).show()
                        exchangeCodeForToken(code)
                    } else {
                        Log.e("Smartcar", "Auth failed: ${smartcarResponse?.toString()}")
                        Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        setContent {
            val user = currentUser
            val repo = repository
            if (user != null && repo != null) {
                App(
                    repository = repo,
                    onConnectSmartcar = { smartcarAuth.launchAuthFlow(this) },
                    onRefreshSmartcar = { refreshSmartcarData() }
                )
            } else {
                SignInScreen(onSignInWithGoogle = { launchGoogleSignIn(dao) })
            }
        }

        scheduleMaintenanceCheck()
        requestNotificationPermissionIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository?.stopListening()
    }

    // FIREBASE

    private fun launchGoogleSignIn(dao: com.example.purincar.data.CarDao) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val signInClient = GoogleSignIn.getClient(this, gso)
        // Sign out first to show account picker
        signInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(signInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val dao = PurinCarDatabase.getDatabase(applicationContext).carDao()
                    repository = PurinCarRepository(dao, FirebaseFirestore.getInstance(), user.uid).also {
                        it.startListening(lifecycleScope)
                    }
                    currentUser = user
                } else {
                    Log.e("Auth", "Firebase credential sign-in failed", task.exception)
                    Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // SMARTCAR API

    private fun securePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            applicationContext,
            "smartcar_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleMaintenanceCheck() {
        val workRequest = PeriodicWorkRequestBuilder<MaintenanceCheckWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "maintenance_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    // TOKEN
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

                val body = tokenRes.body?.string() ?: throw IOException("Empty token response body")
                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = json.getString("refresh_token")

                securePrefs().edit {
                    putString("access_token", accessToken)
                    putString("refresh_token", refreshToken)
                }

                fetchCarDataWithToken(accessToken)

            } catch (e: Exception) {
                Log.e("Smartcar", "Login Error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Login Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun refreshSmartcarData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = securePrefs()
            var accessToken = prefs.getString("access_token", null)
            val refreshToken = prefs.getString("refresh_token", null)

            if (accessToken != null) {
                try {
                    val req = Request.Builder()
                        .url("https://api.smartcar.com/v2.0/vehicles")
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val res = client.newCall(req).execute()

                    if (res.code == 401 && refreshToken != null) {
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
                            val refreshBody = refreshRes.body?.string() ?: throw IOException("Empty refresh response body")
                            val newJson = JSONObject(refreshBody)
                            accessToken = newJson.getString("access_token")
                            val newRefresh = newJson.optString("refresh_token", refreshToken)
                            prefs.edit {
                                putString("access_token", accessToken)
                                putString("refresh_token", newRefresh)
                            }
                            fetchCarDataWithToken(accessToken!!)
                        }
                    } else if (res.isSuccessful) {
                        fetchCarDataWithToken(accessToken)
                    }
                } catch (e: Exception) {
                    Log.e("Smartcar", "Auto-connect failed", e)
                }
            }
        }
    }

    private suspend fun fetchCarDataWithToken(accessToken: String) {
        val repo = repository ?: return
        try {
            val vehiclesRes = client.newCall(
                Request.Builder()
                    .url("https://api.smartcar.com/v2.0/vehicles")
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            ).execute()
            val vehiclesResponse = vehiclesRes.body?.string() ?: throw IOException("Empty vehicles response body")
            val vehicleId = JSONObject(vehiclesResponse).getJSONArray("vehicles").getString(0)

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
                return JSONObject(res.body?.string() ?: "{}")
            }


            // CAR AND ODOMETER
            val info = fetch("")
            val carName = "${info.getInt("year")} ${info.getString("make")} ${info.getString("model")}"
            val odoJson = fetch("odometer")
            val miles = (odoJson.optDouble("distance", 0.0) * 0.621371).toInt()

            val existingCar = repo.getCarBySmartcarId(vehicleId)
            val now = System.currentTimeMillis()
            val carToSave = existingCar?.copy(
                name = carName,
                currentMileage = miles,
                lastSyncedAt = now,
            ) ?: CarEntity(
                name = carName,
                currentMileage = miles,
                smartcarId = vehicleId,
                lastSyncedAt = now,
            )

            if (existingCar != null) repo.updateCar(carToSave) else repo.insertCar(carToSave)

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

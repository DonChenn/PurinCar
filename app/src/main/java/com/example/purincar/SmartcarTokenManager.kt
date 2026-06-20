package com.example.purincar

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Process-wide OkHttp client. Single connection pool, single dispatcher,
 * and timeouts so a hung Smartcar call doesn't block a worker forever.
 */
object HttpClient {
    val instance: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
}

class SmartcarTokenManager(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String
) {
    private val client = HttpClient.instance

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "smartcar_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
        }
    }

    /** Refreshes using the stored refresh token. Saves and returns the new access token, or null on failure. */
    suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val refreshToken = prefs.getString("refresh_token", null) ?: return@withContext null
        try {
            val res = client.newCall(
                Request.Builder()
                    .url("https://auth.smartcar.com/oauth/token")
                    .header("Authorization", Credentials.basic(clientId, clientSecret))
                    .post(
                        FormBody.Builder()
                            .add("grant_type", "refresh_token")
                            .add("refresh_token", refreshToken)
                            .build()
                    )
                    .build()
            ).execute()
            if (!res.isSuccessful) return@withContext null
            val json = JSONObject(res.body?.string() ?: return@withContext null)
            val newAccess = json.getString("access_token")
            val newRefresh = json.optString("refresh_token", refreshToken)
            saveTokens(newAccess, newRefresh)
            newAccess
        } catch (e: Exception) {
            null
        }
    }
}

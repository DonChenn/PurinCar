// Network client setup and the friendly error messages shown when requests fail.
package com.example.purincar.core.network

import com.example.purincar.BuildConfig
import com.example.purincar.core.common.AppConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

object NetworkModule {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Builds the Retrofit client that talks to Smartcar, sharing one connection pool.
    fun createSmartcarApi(): SmartcarApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_CALL_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(AppConfig.SMARTCAR_API_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SmartcarApi::class.java)
    }

    private const val TIMEOUT_CONNECT_SECONDS = 15L
    private const val TIMEOUT_READ_SECONDS = 30L
    private const val TIMEOUT_CALL_SECONDS = 45L
}

// Turns a network or parsing failure into a message a driver can understand.
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException -> "Can't reach the server. Check your internet connection."
    is ConnectException -> "The server refused the connection. Try again in a moment."
    is SocketTimeoutException -> "The server didn't respond in time."
    is SSLException -> "Couldn't establish a secure connection."
    is HttpException -> httpErrorMessage(code())
    is SerializationException -> "The server sent data the app couldn't read."
    else -> message ?: "Something went wrong."
}

// Turns an HTTP status code into a readable message.
fun httpErrorMessage(statusCode: Int): String = when (statusCode) {
    401, 403 -> "Smartcar access expired. Reconnect your car."
    404 -> "Smartcar couldn't find that vehicle."
    429 -> "Smartcar is rate limiting requests. Try again later."
    in 500..599 -> "Smartcar hit an error (HTTP $statusCode)."
    else -> "Smartcar returned HTTP $statusCode."
}

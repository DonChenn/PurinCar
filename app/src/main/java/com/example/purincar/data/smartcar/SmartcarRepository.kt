// Connects the user's car through Smartcar and pulls its name and odometer into the garage.
package com.example.purincar.data.smartcar

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import com.example.purincar.core.common.AppConfig
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.network.SmartcarApi
import com.example.purincar.core.network.toUserMessage
import com.example.purincar.data.car.Car
import com.example.purincar.data.car.CarRepository
import com.example.purincar.data.odometer.OdometerRepository
import com.smartcar.sdk.Mode
import com.smartcar.sdk.SmartcarAuth
import com.smartcar.sdk.SmartcarCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Credentials
import retrofit2.HttpException
import java.time.Instant

@Immutable
sealed interface SmartcarStatus {
    data object Idle : SmartcarStatus

    data object Syncing : SmartcarStatus

    @Immutable
    data class Synced(val carName: String) : SmartcarStatus

    data object NotConnected : SmartcarStatus

    data object LoginCancelled : SmartcarStatus

    @Immutable
    data class Failed(val message: String) : SmartcarStatus
}

class SmartcarRepository(
    private val api: SmartcarApi,
    private val tokens: SmartcarTokenStore,
    private val cars: CarRepository,
    private val odometer: OdometerRepository,
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow<SmartcarStatus>(SmartcarStatus.Idle)
    val status: StateFlow<SmartcarStatus> = _status.asStateFlow()

    private val syncLock = Mutex()

    private val auth = SmartcarAuth(
        AppConfig.SMARTCAR_CLIENT_ID,
        AppConfig.SMARTCAR_REDIRECT_URI,
        AppConfig.SMARTCAR_SCOPES,
        Mode.LIVE,
        SmartcarCallback { response ->
            val code = response?.code
            if (code == null) {
                Log.w(TAG, "Smartcar login ended without a code: $response")
                _status.value = SmartcarStatus.LoginCancelled
            } else {
                scope.launch { connect(code) }
            }
        }
    )

    private val basicAuth: String
        get() = Credentials.basic(AppConfig.SMARTCAR_CLIENT_ID, AppConfig.SMARTCAR_CLIENT_SECRET)

    // Opens Smartcar's login so the user can share their car.
    fun launchConnect(context: Context) {
        auth.launchAuthFlow(context)
    }

    // Pulls the latest name and odometer for the connected car, updating the status as it goes.
    fun refresh() {
        scope.launch { syncVehicle(createIfMissing = true) }
    }

    // Clears a finished status once the screen has shown it.
    fun clearStatus() {
        _status.value = SmartcarStatus.Idle
    }

    // Forgets the Smartcar tokens, as on sign-out.
    suspend fun disconnect() {
        tokens.clear()
        clearStatus()
    }

    // Pulls the connected car into the garage and records today's odometer; only a foreground sync adds new cars and reports its status.
    suspend fun syncVehicle(createIfMissing: Boolean): PurinCarResult<Car>? = syncLock.withLock {
        val report: (SmartcarStatus) -> Unit = { if (createIfMissing) _status.value = it }
        if (tokens.accessToken() == null) {
            report(SmartcarStatus.NotConnected)
            return@withLock null
        }
        report(SmartcarStatus.Syncing)
        try {
            val car = withFreshToken { bearer ->
                val vehicleId = api.getVehicles(bearer).vehicles.firstOrNull()
                    ?: error("No vehicles are shared with PurinCar.")
                val info = api.getVehicle(bearer, vehicleId)
                val miles = (api.getOdometer(bearer, vehicleId).distance * AppConfig.KM_TO_MILES).toInt()
                val saved = cars.upsertFromSmartcar(
                    smartcarId = vehicleId,
                    name = info.displayName(),
                    miles = miles,
                    syncedAt = Instant.now(),
                    createIfMissing = createIfMissing
                )
                if (saved != null && miles > 0) odometer.record(saved.id, miles)
                saved
            }
            report(car?.let { SmartcarStatus.Synced(it.name) } ?: SmartcarStatus.Idle)
            car?.let { PurinCarResult.Success(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smartcar sync failed", e)
            val message = e.toUserMessage()
            report(SmartcarStatus.Failed(message))
            PurinCarResult.Error(message)
        }
    }

    // Trades a login code for tokens, then pulls the car in.
    private suspend fun connect(code: String) {
        _status.value = SmartcarStatus.Syncing
        try {
            val token = api.exchangeCode(basicAuth, code)
            tokens.save(token.accessToken, token.refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Smartcar code exchange failed", e)
            _status.value = SmartcarStatus.Failed(e.toUserMessage())
            return
        }
        syncVehicle(createIfMissing = true)
    }

    // Runs a Smartcar call with the saved token, refreshing it once and retrying if Smartcar says it expired.
    private suspend fun <T> withFreshToken(block: suspend (bearer: String) -> T): T {
        val access = checkNotNull(tokens.accessToken())
        return try {
            block(bearer(access))
        } catch (e: HttpException) {
            if (e.code() != HTTP_UNAUTHORIZED) throw e
            block(bearer(refreshAccessToken() ?: throw e))
        }
    }

    // Swaps the refresh token for a new access token, returning null if Smartcar refuses.
    private suspend fun refreshAccessToken(): String? {
        val refresh = tokens.refreshToken() ?: return null
        return try {
            val token = api.refreshToken(basicAuth, refresh)
            tokens.save(token.accessToken, token.refreshToken)
            token.accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Smartcar token refresh failed", e)
            null
        }
    }

    // Formats a token as a bearer Authorization header.
    private fun bearer(token: String) = "Bearer $token"

    private companion object {
        const val TAG = "SmartcarRepository"
        const val HTTP_UNAUTHORIZED = 401
    }
}

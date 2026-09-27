// Holds the garage's car list and the Smartcar connection status.
package com.example.purincar.feature.garage

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.purincar.PurinCarApp
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.network.toUserMessage
import com.example.purincar.data.auth.AuthRepository
import com.example.purincar.data.car.Car
import com.example.purincar.data.car.CarRepository
import com.example.purincar.data.smartcar.SmartcarRepository
import com.example.purincar.data.smartcar.SmartcarStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GarageUiState(
    val cars: PurinCarResult<List<Car>> = PurinCarResult.Loading,
    val smartcarStatus: SmartcarStatus = SmartcarStatus.Idle
) {
    val connecting: Boolean get() = smartcarStatus == SmartcarStatus.Syncing
}

class GarageViewModel(
    private val cars: CarRepository,
    private val smartcar: SmartcarRepository,
    private val auth: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<GarageUiState> = combine(
        cars.cars()
            .map<List<Car>, PurinCarResult<List<Car>>> { PurinCarResult.Success(it) }
            .catch { emit(PurinCarResult.Error(it.toUserMessage())) },
        smartcar.status
    ) { carList, status -> GarageUiState(cars = carList, smartcarStatus = status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GarageUiState())

    // Opens Smartcar's login to add or update the user's car.
    fun connectSmartcar(context: Context) {
        smartcar.launchConnect(context)
    }

    // Soft-deletes a car from the garage.
    fun deleteCar(id: Long) {
        viewModelScope.launch { cars.delete(id) }
    }

    // Clears the Smartcar message once the snackbar has shown it.
    fun onMessageShown() {
        smartcar.clearStatus()
    }

    // Forgets the Smartcar connection and signs out.
    fun signOut() {
        viewModelScope.launch {
            smartcar.disconnect()
            auth.signOut()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as? PurinCarApp
                    ?: error(
                        "Application is not PurinCarApp. Add " +
                            "android:name=\".PurinCarApp\" to <application> in the manifest."
                    )
                GarageViewModel(app.container.cars, app.container.smartcar, app.container.auth)
            }
        }
    }
}

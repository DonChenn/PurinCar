// Holds one service's history for a car and saves changes to it.
package com.example.purincar.feature.servicehistory

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
import com.example.purincar.data.car.CarRepository
import com.example.purincar.data.maintenance.MaintenanceRecord
import com.example.purincar.data.maintenance.MaintenanceRepository
import com.example.purincar.data.maintenance.ServiceType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class ServiceHistoryUiState(
    val serviceType: ServiceType,
    val records: PurinCarResult<List<MaintenanceRecord>> = PurinCarResult.Loading,
    val currentMileage: Int = 0
) {
    val totalCost: Double get() = (records as? PurinCarResult.Success)?.data?.sumOf { it.cost } ?: 0.0
}

class ServiceHistoryViewModel(
    private val carId: Long,
    private val serviceType: ServiceType,
    cars: CarRepository,
    private val maintenance: MaintenanceRepository
) : ViewModel() {

    val uiState: StateFlow<ServiceHistoryUiState> = combine(
        maintenance.recordsOfType(carId, serviceType)
            .map<List<MaintenanceRecord>, PurinCarResult<List<MaintenanceRecord>>> { PurinCarResult.Success(it) }
            .catch { emit(PurinCarResult.Error(it.toUserMessage())) },
        cars.car(carId)
    ) { records, car ->
        ServiceHistoryUiState(serviceType = serviceType, records = records, currentMileage = car?.currentMileage ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceHistoryUiState(serviceType))

    // Saves a new record for this service.
    fun addRecord(date: LocalDate, mileage: Int, cost: Double, description: String) {
        viewModelScope.launch {
            maintenance.add(
                MaintenanceRecord(
                    carId = carId,
                    serviceType = serviceType,
                    date = date,
                    mileageAtService = mileage,
                    description = description,
                    cost = cost
                )
            )
        }
    }

    // Saves changes to a record.
    fun updateRecord(record: MaintenanceRecord) {
        viewModelScope.launch { maintenance.update(record) }
    }

    // Deletes a record.
    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { maintenance.delete(record) }
    }

    companion object {
        // Builds the view model for one car's service.
        fun factory(carId: Long, serviceType: ServiceType): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as? PurinCarApp
                    ?: error(
                        "Application is not PurinCarApp. Add " +
                            "android:name=\".PurinCarApp\" to <application> in the manifest."
                    )
                ServiceHistoryViewModel(carId, serviceType, app.container.cars, app.container.maintenance)
            }
        }
    }
}

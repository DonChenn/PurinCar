// Holds one car's service status, odometer history and fill-ups, and handles its CSV import and export.
package com.example.purincar.feature.cardetails

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.purincar.PurinCarApp
import com.example.purincar.data.car.Car
import com.example.purincar.data.car.CarRepository
import com.example.purincar.data.csv.buildCsv
import com.example.purincar.data.csv.parseCsv
import com.example.purincar.data.gas.GasRecord
import com.example.purincar.data.gas.GasRepository
import com.example.purincar.data.maintenance.MaintenanceRepository
import com.example.purincar.data.maintenance.ServiceStatus
import com.example.purincar.data.maintenance.serviceStatuses
import com.example.purincar.data.odometer.OdometerReading
import com.example.purincar.data.odometer.OdometerRepository
import com.example.purincar.data.smartcar.SmartcarRepository
import com.example.purincar.data.smartcar.SmartcarStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
sealed interface CsvMessage {
    @Immutable
    data class Imported(val services: Int, val fillUps: Int, val skipped: Int) : CsvMessage

    data object Exported : CsvMessage

    data object Failed : CsvMessage
}

@Immutable
data class CarDetailsUiState(
    val loaded: Boolean = false,
    val car: Car? = null,
    val statuses: List<ServiceStatus> = emptyList(),
    val odometer: List<OdometerReading> = emptyList(),
    val fillUps: List<GasRecord> = emptyList(),
    val smartcarStatus: SmartcarStatus = SmartcarStatus.Idle,
    val csvMessage: CsvMessage? = null
) {
    val refreshing: Boolean get() = smartcarStatus == SmartcarStatus.Syncing
    val totalSpent: Double get() = fillUps.sumOf { it.totalCost }
    val totalGallons: Double get() = fillUps.sumOf { it.gallons }
}

class CarDetailsViewModel(
    private val carId: Long,
    private val cars: CarRepository,
    private val maintenance: MaintenanceRepository,
    private val gas: GasRepository,
    odometer: OdometerRepository,
    private val smartcar: SmartcarRepository
) : ViewModel() {

    private val csvMessage = MutableStateFlow<CsvMessage?>(null)

    val uiState: StateFlow<CarDetailsUiState> = combine(
        combine(cars.car(carId), maintenance.records(carId)) { car, records ->
            car to car?.let { serviceStatuses(it.currentMileage, records) }.orEmpty()
        },
        odometer.readings(carId),
        gas.records(carId),
        smartcar.status,
        csvMessage
    ) { (car, statuses), readings, fillUps, status, message ->
        CarDetailsUiState(
            loaded = true,
            car = car,
            statuses = statuses,
            odometer = readings,
            fillUps = fillUps,
            smartcarStatus = status,
            csvMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarDetailsUiState())

    // Pulls the latest name and odometer from Smartcar.
    fun refreshSmartcar() {
        smartcar.refresh()
    }

    // Adds the services and fill-ups in a CSV file and raises the odometer to the highest mileage in it.
    fun importCsv(text: String) {
        viewModelScope.launch {
            val parsed = parseCsv(text, carId)
            maintenance.addAll(parsed.services)
            gas.addAll(parsed.fillUps)
            if (parsed.highestMileage > 0) cars.raiseMileage(carId, parsed.highestMileage)
            csvMessage.value = CsvMessage.Imported(parsed.services.size, parsed.fillUps.size, parsed.skippedRows)
        }
    }

    // Builds a CSV file of every service and fill-up for the car.
    suspend fun exportCsv(): String = buildCsv(maintenance.recordsOnce(carId), gas.recordsOnce(carId))

    // Reports whether reading or writing a CSV file worked.
    fun onCsvFileResult(exported: Boolean) {
        csvMessage.value = if (exported) CsvMessage.Exported else CsvMessage.Failed
    }

    // Saves a new fill-up.
    fun addFillUp(date: LocalDate, gallons: Double, totalCost: Double, notes: String) {
        viewModelScope.launch {
            gas.add(GasRecord(carId = carId, date = date, gallons = gallons, totalCost = totalCost, notes = notes))
        }
    }

    // Saves changes to a fill-up.
    fun updateFillUp(record: GasRecord) {
        viewModelScope.launch { gas.update(record) }
    }

    // Deletes a fill-up.
    fun deleteFillUp(record: GasRecord) {
        viewModelScope.launch { gas.delete(record) }
    }

    // Clears the Smartcar message once the snackbar has shown it.
    fun onSmartcarMessageShown() {
        smartcar.clearStatus()
    }

    // Clears the CSV message once the snackbar has shown it.
    fun onCsvMessageShown() {
        csvMessage.value = null
    }

    companion object {
        // Builds the view model for one car.
        fun factory(carId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as? PurinCarApp
                    ?: error(
                        "Application is not PurinCarApp. Add " +
                            "android:name=\".PurinCarApp\" to <application> in the manifest."
                    )
                with(app.container) {
                    CarDetailsViewModel(carId, cars, maintenance, gas, odometer, smartcar)
                }
            }
        }
    }
}

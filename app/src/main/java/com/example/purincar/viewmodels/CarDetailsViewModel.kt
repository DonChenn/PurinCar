package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.CarEntity
import com.example.purincar.data.GasRecord
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.data.OdometerReading
import com.example.purincar.data.repository.PurinCarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class ServiceStatus(
    val name: String,
    val mileageProgress: Float,
    val timeProgress: Float,
    val mileageText: String,
    val timeText: String
)

class CarDetailsViewModel(
    private val repository: PurinCarRepository,
    private val carId: Int
) : ViewModel() {

    private val serviceTypes = listOf(
        "Engine Oil", "Air Filters", "Engine Coolant", "Brake Fluid",
        "Battery Fan", "Transmission Fluid", "Spark Plugs", "Miscellaneous"
    )

    private val mileageIntervals = mapOf(
        "Engine Oil" to 5000, "Air Filters" to 15000, "Engine Coolant" to 30000,
        "Brake Fluid" to 30000, "Battery Fan" to 30000, "Transmission Fluid" to 60000,
        "Spark Plugs" to 100000, "Miscellaneous" to 0
    )

    private val timeIntervals = mapOf(
        "Engine Oil" to 180, "Air Filters" to 365, "Engine Coolant" to 730,
        "Brake Fluid" to 730, "Battery Fan" to 1095, "Transmission Fluid" to 1460,
        "Spark Plugs" to 1825, "Miscellaneous" to 0
    )

    val carInfo: Flow<CarEntity?> = repository.getAllCars().map { list ->
        list.find { it.id == carId }
    }

    private val allRecords: Flow<List<MaintenanceRecord>> = repository.getRecordsForCar(carId)
    private val allGasRecords: Flow<List<GasRecord>> = repository.getGasRecordsForCar(carId)

    val odometerHistory: Flow<List<OdometerReading>> = repository.getOdometerReadingsForCar(carId)

    // Scroll position of the service-records list, kept here so it survives
    // navigating to ServiceHistory and back (this ViewModel outlives the screen
    // while its nav entry stays on the back stack — including predictive back).
    var recordsScrollIndex: Int = 0
        private set
    var recordsScrollOffset: Int = 0
        private set

    fun saveRecordsScroll(index: Int, offset: Int) {
        recordsScrollIndex = index
        recordsScrollOffset = offset
    }

    val serviceStatuses: StateFlow<List<ServiceStatus>> = combine(carInfo, allRecords) { car, records ->
        val currentMileage = car?.currentMileage ?: 0
        val currentDate = LocalDate.now()

        serviceTypes.map { type ->
            val lastRecord =
                records.filter { it.serviceType == type }.maxByOrNull { it.mileageAtService }
            val lastMileage = lastRecord?.mileageAtService ?: 0
            val mInterval = mileageIntervals[type] ?: 5000
            val mDriven = (currentMileage - lastMileage).coerceAtLeast(0)
            val mProgress = if (mInterval > 0) (mDriven.toFloat() / mInterval.toFloat()).coerceIn(
                0f,
                1f
            ) else -1f

            val lastDateStr = lastRecord?.date
            val daysElapsed = if (lastDateStr != null) {
                try {
                    val lastDate = LocalDate.parse(lastDateStr)
                    ChronoUnit.DAYS.between(lastDate, currentDate).toInt().coerceAtLeast(0)
                } catch (e: DateTimeParseException) {
                    0
                }
            } else {
                0
            }

            val tInterval = timeIntervals[type] ?: 365
            val tProgress =
                if (tInterval > 0) (daysElapsed.toFloat() / tInterval.toFloat()).coerceIn(
                    0f,
                    1f
                ) else -1f

            ServiceStatus(
                name = type,
                mileageProgress = mProgress,
                timeProgress = tProgress,
                mileageText = "$mDriven / $mInterval mi",
                timeText = "$daysElapsed / $tInterval days"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateMileage(newMileage: Int) {
        viewModelScope.launch {
            val currentCar = carInfo.firstOrNull()
            if (currentCar != null) {
                repository.updateCar(currentCar.copy(currentMileage = newMileage))
                repository.recordOdometerReading(
                    carId, newMileage, LocalDate.now().toString()
                )
            }
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            val lines = csvContent.lines()
            var maxMileageFound = 0
            val currentCar = carInfo.firstOrNull()
            if (currentCar != null) maxMileageFound = currentCar.currentMileage

            val dataLines =
                if (lines.isNotEmpty() && lines[0].startsWith("Type")) lines.drop(1) else lines

            dataLines.forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())

                if (parts.size >= 3) {
                    val type = parts[0].trim()
                    val date = parts[1].trim()

                    if (type.equals("Gas", ignoreCase = true)) {
                        val cost = parts.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.0
                        var notes = parts.getOrNull(4)?.trim() ?: ""
                        notes = notes.removeSurrounding("\"").replace("\"\"", "\"")
                        val gallons = parts.getOrNull(5)?.trim()?.toDoubleOrNull() ?: 0.0
                        repository.insertGasRecord(GasRecord(carId = carId, date = date, gallons = gallons, totalCost = cost, notes = notes))
                    } else {
                        val mileage = parts[2].trim().toIntOrNull() ?: 0
                        val cost = parts.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.0
                        var description = parts.getOrNull(4)?.trim() ?: ""
                        description = description.removeSurrounding("\"").replace("\"\"", "\"")

                        if (mileage > maxMileageFound) maxMileageFound = mileage

                        val appServiceType = serviceTypes.find {
                            it.equals(type, ignoreCase = true) || it.contains(type, ignoreCase = true)
                        }
                        if (appServiceType != null) {
                            repository.insertRecord(
                                MaintenanceRecord(
                                    carId = carId,
                                    serviceType = appServiceType,
                                    date = date,
                                    mileageAtService = mileage,
                                    cost = cost,
                                    description = description
                                )
                            )
                        }
                    }
                }
            }
            if (currentCar != null && maxMileageFound > currentCar.currentMileage) {
                repository.updateCar(currentCar.copy(currentMileage = maxMileageFound))
            }
        }
    }

    suspend fun generateCsvExport(): String {
        val records = allRecords.first()
        val gasRecords = allGasRecords.first()
        val sb = StringBuilder()

        sb.append("Type,Date,Mileage,Cost,Description,Gallons\n")

        records.forEach { record ->
            var desc = record.description.replace("\"", "\"\"")
            if (desc.contains(",")) desc = "\"$desc\""
            sb.append("${record.serviceType},${record.date},${record.mileageAtService},${record.cost},$desc,\n")
        }

        gasRecords.forEach { record ->
            var notes = record.notes.replace("\"", "\"\"")
            if (notes.contains(",")) notes = "\"$notes\""
            sb.append("Gas,${record.date},,${record.totalCost},$notes,${record.gallons}\n")
        }

        return sb.toString()
    }
}

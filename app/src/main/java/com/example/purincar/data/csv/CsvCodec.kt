// Reads and writes the service and fill-up CSV files people import and export.
package com.example.purincar.data.csv

import androidx.compose.runtime.Immutable
import com.example.purincar.data.gas.GasRecord
import com.example.purincar.data.maintenance.MaintenanceRecord
import com.example.purincar.data.maintenance.ServiceType
import java.time.LocalDate

@Immutable
data class CsvImport(
    val services: List<MaintenanceRecord>,
    val fillUps: List<GasRecord>,
    val skippedRows: Int
) {
    val highestMileage: Int get() = services.maxOfOrNull { it.mileageAtService } ?: 0
}

private const val HEADER = "Type,Date,Mileage,Cost,Description,Gallons"
private const val GAS_TYPE = "Gas"
private val FieldSplitter = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")

// Parses a CSV export into service records and fill-ups for a car, counting rows it couldn't read.
fun parseCsv(text: String, carId: Long): CsvImport {
    val services = mutableListOf<MaintenanceRecord>()
    val fillUps = mutableListOf<GasRecord>()
    var skipped = 0

    text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("Type") }
        .forEach { line ->
            val fields = line.split(FieldSplitter).map { it.trim() }
            val date = fields.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (fields.size < 3 || date == null) {
                skipped++
                return@forEach
            }
            val type = fields[0]
            val cost = fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val note = fields.getOrNull(4)?.unquote().orEmpty()

            if (type.equals(GAS_TYPE, ignoreCase = true)) {
                fillUps += GasRecord(
                    carId = carId,
                    date = date,
                    gallons = fields.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                    totalCost = cost,
                    notes = note
                )
            } else {
                val serviceType = ServiceType.fromLabel(type)
                if (serviceType == null) {
                    skipped++
                    return@forEach
                }
                services += MaintenanceRecord(
                    carId = carId,
                    serviceType = serviceType,
                    date = date,
                    mileageAtService = fields[2].toIntOrNull() ?: 0,
                    cost = cost,
                    description = note
                )
            }
        }

    return CsvImport(services = services, fillUps = fillUps, skippedRows = skipped)
}

// Writes a car's service records and fill-ups as a CSV file.
fun buildCsv(services: List<MaintenanceRecord>, fillUps: List<GasRecord>): String = buildString {
    appendLine(HEADER)
    services.forEach { record ->
        appendLine(
            "${record.serviceType.label},${record.date},${record.mileageAtService},${record.cost}," +
                "${record.description.quoteIfNeeded()},"
        )
    }
    fillUps.forEach { record ->
        appendLine("$GAS_TYPE,${record.date},,${record.totalCost},${record.notes.quoteIfNeeded()},${record.gallons}")
    }
}

// Removes CSV quoting from a field.
private fun String.unquote(): String = removeSurrounding("\"").replace("\"\"", "\"")

// Escapes quotes and wraps the field in quotes if it holds a comma.
private fun String.quoteIfNeeded(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.contains(',')) "\"$escaped\"" else escaped
}

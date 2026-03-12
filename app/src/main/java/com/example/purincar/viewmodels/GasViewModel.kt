package com.example.purincar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purincar.data.GasRecord
import com.example.purincar.data.repository.PurinCarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GasViewModel(
    private val repository: PurinCarRepository,
    private val carId: Int
) : ViewModel() {

    val gasRecords: Flow<List<GasRecord>> = repository.getGasRecordsForCar(carId)

    fun addRecord(date: String, gallons: Double, totalCost: Double, notes: String) {
        viewModelScope.launch {
            repository.insertGasRecord(GasRecord(carId = carId, date = date, gallons = gallons, totalCost = totalCost, notes = notes))
        }
    }

    fun updateRecord(record: GasRecord) {
        viewModelScope.launch { repository.updateGasRecord(record) }
    }

    fun deleteRecord(record: GasRecord) {
        viewModelScope.launch { repository.deleteGasRecord(record) }
    }
}

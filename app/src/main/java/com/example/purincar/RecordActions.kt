package com.example.purincar

sealed class RecordActions {
    data class AddRecord(val record: CarRecord) : RecordActions
}
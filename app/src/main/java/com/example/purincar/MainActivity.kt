package com.example.purincar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.purincar.data.PurinCarDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PurinCarDatabase.getDatabase(applicationContext)
        val carDao = database.carDao()

        setContent {
            App(dao = carDao)
        }
    }
}
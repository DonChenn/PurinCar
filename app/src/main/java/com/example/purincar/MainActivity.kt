package com.example.purincar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.purincar.ui.theme.PurinCarTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurinCarTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "cars") {
                    composable("cars") {
                        CarSelectionScreen(navController)
                    }

                    composable("records/{carId}") { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        if (carId != null) {
                            CarRecordsListScreen(carId)
                        }
                    }

                    composable("records/{carId}/{recordType}") { backStackEntry ->
                        val carId = backStackEntry.arguments?.getString("carId")
                        val recordType = backStackEntry.arguments?.getString("recordType")
                        if (carId != null && recordType != null) {
                            CarRecordScreen(carId, recordType)
                        }
                    }
                }
            }
        }
    }
}

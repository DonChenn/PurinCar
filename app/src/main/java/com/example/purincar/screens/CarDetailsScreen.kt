package com.example.purincar.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.purincar.viewmodels.CarDetailsViewModel

@Composable
fun CarDetailsScreen(
    car: String,
    backStack: NavBackStack<NavKey>,
    viewModel: CarDetailsViewModel = viewModel {
        CarDetailsViewModel(car)
    }
) {
    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Car Details Screen for $car",
        )
    }
}
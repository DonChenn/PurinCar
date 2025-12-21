package com.example.purincar.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.viewmodels.CarDetails
import com.example.purincar.viewmodels.CarDetailsViewModel

@Composable
fun CarDetailsScreen(
    car: CarDetails,
    viewModel: CarDetailsViewModel = viewModel {
        CarDetailsViewModel(car)
    }
) {
    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Car Details Screen for ${car.name}",
        )
    }
}
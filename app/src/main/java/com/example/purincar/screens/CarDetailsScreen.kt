package com.example.purincar.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Car: ${car.name}", fontSize = 24.sp, color = PurinBrown)
            Text(text = "Mileage: ${car.mileage}", fontSize = 20.sp, color = PurinBrown)
        }
    }
}

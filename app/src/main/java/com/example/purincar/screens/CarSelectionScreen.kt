package com.example.purincar.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.viewmodels.CarDetails
import com.example.purincar.viewmodels.CarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarSelectionScreen(
    viewModel: CarViewModel,
    onCarClick: (CarDetails) -> Unit
) {
    val cars by viewModel.cars.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newCarInput by remember { mutableStateOf("") }
    var carMileage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var carToDelete by remember { mutableStateOf<CarDetails?>(null) }

    if(cars.isNotEmpty()) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp, 64.dp, 16.dp, 64.dp),
        ) {
            items(cars.size) { currentCar ->
                val car = cars[currentCar]

                Card(
                    onClick = { onCarClick(car) },
                    colors = CardDefaults.cardColors(containerColor = PurinBrown),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = car.name,
                            fontSize = 24.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Delete Button
                        IconButton(
                            onClick = {
                                carToDelete = car
                                showDeleteDialog = true
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Car",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            Text(
                text = "Please enter a car",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 16.dp, 16.dp, 40.dp)
    ) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd),
            shape = CircleShape,
            containerColor = PurinBrown
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add a car",
                tint = Color.White
            )
        }
    }

    if(showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Add a car") },
            text = {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = newCarInput,
                        onValueChange = { newCarInput = it },
                        placeholder = { Text(text = "Car name") }
                    )

                    TextField(
                        value = carMileage,
                        onValueChange = { carMileage = it },
                        placeholder = { Text(text = "Car mileage") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCarInput.isNotBlank() && carMileage.isNotBlank()) {
                            val car = CarDetails(name = newCarInput, currentMileage = carMileage.toInt())
                            viewModel.addCar(car)
                            newCarInput = ""
                            carMileage = ""
                            showDialog = false
                        }
                    }
                ) {
                    Text("Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Car?") },
            text = { Text("Are you sure you want to delete \"${carToDelete?.name}\"? All service records will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        carToDelete?.let { viewModel.removeCar(it) }
                        showDeleteDialog = false
                        carToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

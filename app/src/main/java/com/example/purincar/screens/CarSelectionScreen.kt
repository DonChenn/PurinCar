package com.example.purincar.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.viewmodels.CarDetails
import com.example.purincar.viewmodels.CarViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CarSelectionScreen(
    viewModel: CarViewModel,
    onCarClick: (CarDetails) -> Unit,
    onConnectSmartcar: () -> Unit
) {
    val cars by viewModel.cars.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<CarDetails?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. CAR LIST
        if (cars.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tap + to import your car", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(cars) { car ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onCarClick(car) },
                                onLongClick = { showDeleteDialog = car }
                            ),
                        colors = CardDefaults.cardColors(containerColor = PurinBrown),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = car.name,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. THE PLUS BUTTON
        FloatingActionButton(
            onClick = onConnectSmartcar,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            containerColor = PurinBrown,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, "Import Car", tint = Color.White)
        }
    }

    // DELETE
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Car") },
            text = { Text("Are you sure you want to delete ${showDeleteDialog?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { viewModel.removeCar(it) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color.Black)
                }
            }
        )
    }
}

package com.example.purincar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel
) {
    val car by viewModel.carInfo.collectAsState(initial = null)
    var selectedServiceType by remember { mutableStateOf<String?>(null) }

    // State for mileage editing
    var isEditingMileage by remember { mutableStateOf(false) }
    var mileageInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Update local state when car data loads
    LaunchedEffect(car) {
        car?.let {
            mileageInput = it.currentMileage.toString()
        }
    }

    if (selectedServiceType != null) {
        ServiceDetailDialog(
            serviceType = selectedServiceType!!,
            viewModel = viewModel,
            onDismiss = { selectedServiceType = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurinYellow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header: Car Name and Editable Mileage ---
        Text(
            text = car?.name ?: "Loading...",
            fontSize = 32.sp,
            color = PurinBrown,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Mileage: ", fontSize = 20.sp, color = Color.Black)

            if (isEditingMileage) {
                OutlinedTextField(
                    value = mileageInput,
                    onValueChange = { mileageInput = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        val newMileage = mileageInput.toIntOrNull()
                        if (newMileage != null && newMileage != car?.currentMileage) {
                            viewModel.updateMileage(newMileage)
                        }
                        isEditingMileage = false
                        focusManager.clearFocus()
                    })
                )
                IconButton(onClick = {
                    val newMileage = mileageInput.toIntOrNull()
                    if (newMileage != null && newMileage != car?.currentMileage) {
                        viewModel.updateMileage(newMileage)
                    }
                    isEditingMileage = false
                    focusManager.clearFocus()
                }) {
                    Text("Save", color = PurinBrown, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "${car?.currentMileage ?: 0}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isEditingMileage = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Mileage", tint = PurinBrown)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = PurinBrown)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(viewModel.serviceTypes) { serviceType ->
                val progress = viewModel.getProgressForType(serviceType, car?.currentMileage ?: 0)

                ServiceTypeItem(
                    name = serviceType,
                    progress = progress,
                    onClick = { selectedServiceType = serviceType }
                )
            }
        }
    }
}

@Composable
fun ServiceTypeItem(
    name: String,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Large clickable area
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PurinBrown),
        shape = RoundedCornerShape(16.dp) // Matching the rounded look
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${(progress * 100).toInt()}% Used",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = PurinYellow,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
fun ServiceDetailDialog(
    serviceType: String,
    viewModel: CarDetailsViewModel,
    onDismiss: () -> Unit
) {
    val history by viewModel.getRecordsForType(serviceType).collectAsState(initial = emptyList())

    var showAddForm by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf("") }
    var mileageInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = serviceType) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                if (showAddForm) {
                    Text("Add New Record", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mileageInput,
                        onValueChange = { mileageInput = it },
                        label = { Text("Mileage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddForm = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val m = mileageInput.toIntOrNull()
                                if (dateInput.isNotBlank() && m != null) {
                                    viewModel.addRecord(serviceType, dateInput, m)
                                    showAddForm = false
                                    dateInput = ""
                                    mileageInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                } else {
                    // Button to toggle add form
                    Button(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Service Record")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("History", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (history.isEmpty()) {
                        Text("No records found.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn {
                            items(history) { record ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(record.date, fontWeight = FontWeight.Medium)
                                        Text("${record.mileageAtService} mi")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showAddForm) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

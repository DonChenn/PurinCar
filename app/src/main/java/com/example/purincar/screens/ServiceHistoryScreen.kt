package com.example.purincar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.ServiceHistoryViewModel
import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.DateRange
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    viewModel: ServiceHistoryViewModel
) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val currentCarMileage by viewModel.currentMileage.collectAsState(initial = 0)
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PurinBrown,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PurinYellow)
                .padding(16.dp)
        ) {
            Text(
                text = viewModel.serviceType,
                fontSize = 28.sp,
                color = PurinBrown,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = PurinBrown)
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { record ->

                        var showDeleteButton by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = PurinBrown),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (showDeleteButton) {
                                            showDeleteButton = false
                                        }
                                    },
                                    onLongClick = {
                                        showDeleteButton = true
                                    }
                                )
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)) {

                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                    Text(text = record.date, color = Color.White, fontSize = 16.sp)
                                    Text(
                                        text = "${record.mileageAtService} miles",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (record.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = record.description,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }

                                if (showDeleteButton) {
                                    IconButton(
                                        onClick = {
                                            recordToDelete = record
                                            showDeleteDialog = true
                                            showDeleteButton = false
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 12.dp, y = (-12).dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Record",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var dateInput by remember { mutableStateOf("") }
        var mileageInput by remember { mutableStateOf("") }
        var descriptionInput by remember { mutableStateOf("") }

        // Date Calendar Formatting

        val context = LocalContext.current
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = "$year-${
                    (month + 1).toString()
                        .padStart(2, '0')
                }-${
                    dayOfMonth.toString()
                        .padStart(2, '0')
                }"
                dateInput = formattedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Add service record alert dialog

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Service Record") },
            text = {
                Column {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mileageInput,
                        onValueChange = { mileageInput = it },
                        label = { Text("Mileage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(currentCarMileage.toString()) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Description (Notes, Parts, etc.)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = mileageInput.toIntOrNull()
                        if (dateInput.isNotBlank() && m != null) {
                            viewModel.addRecord(dateInput, m, descriptionInput)
                            showAddDialog = false
                        }
                    },
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record?") },
            text = { Text("Are you sure you want to delete this service record?") },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { viewModel.removeRecord(it) }
                        showDeleteDialog = false
                        recordToDelete = null
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

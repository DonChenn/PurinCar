package com.example.purincar.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.ServiceHistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

private fun formatDate(isoDate: String): String = try {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (e: Exception) { isoDate }

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun ServiceHistoryScreen(
    viewModel: ServiceHistoryViewModel
) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val currentCarMileage by viewModel.currentMileage.collectAsState(initial = 0)

    // UI States
    var showEntryDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Data States
    var selectedRecord by remember { mutableStateOf<MaintenanceRecord?>(null) }

    // Calculate total cost for the header (fixes the "null" issue)
    val totalAccumulatedCost = history.sumOf { it.cost }

    // Form States
    var dateInput by remember { mutableStateOf("") }
    var mileageInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf(currencyFieldValue("")) }
    var descriptionInput by remember { mutableStateOf("") }

    fun openAddDialog() {
        selectedRecord = null
        dateInput = ""
        mileageInput = ""
        costInput = currencyFieldValue("")
        descriptionInput = ""
        showEntryDialog = true
    }

    fun openEditDialog(record: MaintenanceRecord) {
        selectedRecord = record
        dateInput = record.date
        mileageInput = record.mileageAtService.toString()
        costInput = currencyFieldValue(centsPrefill(record.cost))
        descriptionInput = record.description
        showEntryDialog = true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openAddDialog() },
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

            // Header showing total accumulated costs for this service type
            Text(
                text = "Accumulated Costs: $${"%.2f".format(totalAccumulatedCost)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
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
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PurinBrown),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* Could expand details */ },
                                    onLongClick = {
                                        selectedRecord = record
                                        showOptionsDialog = true
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatDate(record.date),
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                        // Display individual record cost
                                        if (record.cost > 0) {
                                            Text(
                                                text = "$${"%.2f".format(record.cost)}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

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
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Long Press Options Dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Options") },
            text = { Text("Choose an action for this record.") },
            confirmButton = {
                TextButton(onClick = {
                    showOptionsDialog = false
                    selectedRecord?.let { openEditDialog(it) }
                }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOptionsDialog = false
                    showDeleteDialog = true
                }) {
                    Text("Delete", color = Color.Red)
                }
            }
        )
    }

    // 2. Add / Edit Dialog
    if (showEntryDialog) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = "$year-${(month + 1).toString().padStart(2, '0')}-${
                    dayOfMonth.toString().padStart(2, '0')
                }"
                dateInput = formattedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showEntryDialog = false },
            title = { Text(if (selectedRecord == null) "Add Service Record" else "Edit Service Record") },
            text = {
                Column {
                    OutlinedTextField(
                        value = if (dateInput.isNotBlank()) formatDate(dateInput) else "",
                        onValueChange = { },
                        label = { Text("Date") },
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
                        value = costInput,
                        onValueChange = { costInput = currencyFieldValue(formatCentsInput(it.text)) },
                        label = { Text("Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
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
                        val c = costInput.text.toDoubleOrNull() ?: 0.0

                        if (dateInput.isNotBlank() && m != null) {
                            if (selectedRecord == null) {
                                viewModel.addRecord(dateInput, m, c, descriptionInput)
                            } else {
                                val updated = selectedRecord!!.copy(
                                    date = dateInput,
                                    mileageAtService = m,
                                    cost = c,
                                    description = descriptionInput
                                )
                                viewModel.updateRecord(updated)
                            }
                            showEntryDialog = false
                        }
                    },
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEntryDialog = false }) { Text("Cancel") }
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
                        selectedRecord?.let { viewModel.removeRecord(it) }
                        showDeleteDialog = false
                        selectedRecord = null
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

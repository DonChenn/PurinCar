package com.example.purincar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.purincar.data.MaintenanceRecord
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel

@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel
) {
    val records by viewModel.records.collectAsState()
    val car by viewModel.carInfo.collectAsState(initial = null)
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = PurinBrown
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PurinYellow)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = car?.name ?: "Loading...", fontSize = 28.sp, color = PurinBrown, fontWeight = FontWeight.Bold)
            Text(text = "Current Mileage: ${car?.currentMileage ?: 0}", fontSize = 18.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Maintenance History", fontSize = 20.sp, modifier = Modifier.align(Alignment.Start))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(records) { record ->
                    RecordItem(record)
                }
            }
        }
    }

    if (showDialog) {
        AddRecordDialog(
            onDismiss = { showDialog = false },
            onConfirm = { type, date, miles ->
                viewModel.addRecord(type, date, miles.toIntOrNull() ?: 0)
                showDialog = false
            }
        )
    }
}

@Composable
fun RecordItem(record: MaintenanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PurinBrown)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = record.serviceType, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(text = record.date, fontSize = 14.sp, color = Color.White)
            }
            Text(text = "${record.mileageAtService} mi", fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun AddRecordDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var type by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var miles by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service Record") },
        text = {
            Column {
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Service Type (e.g. Oil)") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = miles, onValueChange = { miles = it }, label = { Text("Mileage") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(type, date, miles) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
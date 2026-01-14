package com.example.purincar.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel
import com.example.purincar.viewmodels.ServiceStatus
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel,
    onServiceClick: (String) -> Unit
) {
    val car by viewModel.carInfo.collectAsState(initial = null)
    val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // CSV Import Logic (Kept this as it's useful for backups)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val content = BufferedReader(InputStreamReader(inputStream)).readText()
                    viewModel.importCsv(content)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // CSV Export Logic
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val csvData = viewModel.generateCsvExport()
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvData.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurinYellow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Car Name
        Text(
            text = car?.name ?: "Loading...",
            fontSize = 32.sp,
            color = PurinBrown,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Mileage Display (Read-Only)
        Text(
            text = "Mileage: ${car?.currentMileage ?: 0} miles",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. CSV Buttons (Optional, kept for utility)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { importLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
            ) {
                Text("Import CSV", color = Color.White)
            }

            Button(
                onClick = { exportLauncher.launch("car_records_export.csv") },
                colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
            ) {
                Text("Export CSV", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = PurinBrown)
        Spacer(modifier = Modifier.height(16.dp))

        // 4. Service Status List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(serviceStatuses) { status ->
                ServiceStatusItem(
                    status = status,
                    onClick = { onServiceClick(status.name) }
                )
            }
        }
    }
}

@Composable
fun ServiceStatusItem(
    status: ServiceStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PurinBrown),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = status.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mileage Progress Bar
            if (status.mileageProgress >= 0f) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Mileage", color = Color.White, fontSize = 12.sp)
                    Text(text = status.mileageText, color = Color.White, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { status.mileageProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = when {
                        status.mileageProgress > 0.9f -> Color.Red
                        status.mileageProgress > 0.6f -> Color.Yellow
                        else -> Color.Green
                    },
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Progress Bar
            if (status.timeProgress >= 0f) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Time", color = Color.White, fontSize = 12.sp)
                    Text(text = status.timeText, color = Color.White, fontSize = 12.sp)
                }

                LinearProgressIndicator(
                    progress = { status.timeProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = when {
                        status.timeProgress > 0.9f -> Color.Red
                        status.timeProgress > 0.6f -> Color.Yellow
                        else -> Color.Green
                    },
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

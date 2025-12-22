package com.example.purincar.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel
import com.example.purincar.viewmodels.ServiceStatus
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel,
    onServiceClick: (String) -> Unit
) {
    val car by viewModel.carInfo.collectAsState(initial = null)
    val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())

    var isEditingMileage by remember { mutableStateOf(false) }
    var mileageInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
                if (content != null) {
                    viewModel.importCsv(content)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    LaunchedEffect(car) {
        car?.let { mileageInput = it.currentMileage.toString() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurinYellow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    onValueChange = { mileageInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PurinBrown)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { importLauncher.launch("text/csv") },
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Mileage", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(text = status.mileageText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = { status.mileageProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = PurinYellow,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Time", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(text = status.timeText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = { status.timeProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color.Cyan.copy(alpha = 0.7f),
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

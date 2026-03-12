package com.example.purincar.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Refresh
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.input.KeyboardType
import java.util.Calendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.data.CarEntity
import com.example.purincar.data.GasRecord
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel
import com.example.purincar.viewmodels.GasViewModel
import com.example.purincar.viewmodels.ServiceStatus
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.LayoutDirection

private fun formatDate(isoDate: String): String = try {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (e: Exception) { isoDate }

@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel,
    gasViewModel: GasViewModel,
    onServiceClick: (String) -> Unit,
    onRefreshSmartcar: () -> Unit
) {
    val car by viewModel.carInfo.collectAsState(initial = null)
    val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PurinYellow,
        bottomBar = {
            NavigationBar(containerColor = PurinBrown) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Status",
                            tint = Color.White
                        )
                    },
                    label = { Text("Status", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Black.copy(
                            alpha = 0.2f
                        )
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Records",
                            tint = Color.White
                        )
                    },
                    label = { Text("Records", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Black.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.LocalGasStation,
                            contentDescription = "Gas",
                            tint = Color.White
                        )
                    },
                    label = { Text("Gas", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Black.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = paddingValues.calculateBottomPadding(),
                    top = 16.dp
                )
        ) {
            when (selectedTab) {
                0 -> VehicleStatusTab(car, onRefreshSmartcar)
                1 -> ServiceRecordsTab(car, serviceStatuses, viewModel, onServiceClick)
                2 -> GasTab(gasViewModel)
            }
        }
    }
}

@Composable
fun CarHeader(car: CarEntity?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = car?.name ?: "Loading...",
            fontSize = 28.sp,
            color = PurinBrown,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Text(
            text = "odometer: ${car?.currentMileage ?: 0} miles",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun VehicleStatusTab(car: CarEntity?, onRefreshSmartcar: () -> Unit) {
    val lastSyncedText = car?.lastSyncedAt?.let { millis ->
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(millis))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarHeader(car)
        HorizontalDivider(color = PurinBrown)

        Card(
            colors = CardDefaults.cardColors(containerColor = PurinBrown),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vehicle Health",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (car?.smartcarId != null) {
                        IconButton(onClick = onRefreshSmartcar) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (lastSyncedText != null) {
                    Text(
                        text = "Last updated: $lastSyncedText",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        StatusItem("Doors", if (car?.isLocked == true) "Locked" else "Unlocked")
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        StatusItem(
                            "Fuel",
                            "${((car?.fuelPercent ?: 0.0) * 100).toInt()}%",
                            isRightAligned = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ServiceRecordsTab(
    car: CarEntity?,
    serviceStatuses: List<ServiceStatus>,
    viewModel: CarDetailsViewModel,
    onServiceClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        viewModel.importCsv(
                            BufferedReader(
                                InputStreamReader(inputStream)
                            ).readText()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            uri?.let {
                scope.launch {
                    val csvData = viewModel.generateCsvExport(); try {
                    context.contentResolver.openOutputStream(it)
                        ?.use { outputStream -> outputStream.write(csvData.toByteArray()) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                }
            }
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        item { CarHeader(car) }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { importLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
                    ) { Text("Import CSV", color = Color.White) }
                    Button(
                        onClick = { exportLauncher.launch("car_records_export.csv") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)
                    ) { Text("Export CSV", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = PurinBrown)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        items(serviceStatuses) { status ->
            ServiceStatusItem(
                status = status,
                onClick = { onServiceClick(status.name) })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, isRightAligned: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GasTab(viewModel: GasViewModel) {
    val records by viewModel.gasRecords.collectAsState(initial = emptyList())
    var showEntryDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<GasRecord?>(null) }

    val totalSpent = records.sumOf { it.totalCost }
    val totalGallons = records.sumOf { it.gallons }

    var dateInput by remember { mutableStateOf("") }
    var gallonsInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    fun openAddDialog() {
        selectedRecord = null
        dateInput = ""; gallonsInput = ""; costInput = ""; notesInput = ""
        showEntryDialog = true
    }

    fun openEditDialog(record: GasRecord) {
        selectedRecord = record
        dateInput = record.date
        gallonsInput = record.gallons.toString()
        costInput = record.totalCost.toString()
        notesInput = record.notes
        showEntryDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Spent: $${"%.2f".format(totalSpent)}",
                        fontSize = 28.sp,
                        color = PurinBrown,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "Total Gallons: ${"%.2f".format(totalGallons)} gal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = PurinBrown)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(records) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PurinBrown),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { selectedRecord = record; showOptionsDialog = true }
                        )
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDate(record.date), color = Color.White, fontSize = 16.sp)
                                Text("$${"%.2f".format(record.totalCost)}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("${"%.3f".format(record.gallons)} gal", color = Color.White, fontWeight = FontWeight.Bold)
                            if (record.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(record.notes, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        FloatingActionButton(
            onClick = { openAddDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = PurinBrown,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add gas record", tint = Color.White)
        }
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Options") },
            text = { Text("Choose an action for this record.") },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false; selectedRecord?.let { openEditDialog(it) } }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false; showDeleteDialog = true }) {
                    Text("Delete", color = Color.Red)
                }
            }
        )
    }

    if (showEntryDialog) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                dateInput = "$year-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showEntryDialog = false },
            title = { Text(if (selectedRecord == null) "Add Gas Record" else "Edit Gas Record") },
            text = {
                Column {
                    OutlinedTextField(
                        value = if (dateInput.isNotBlank()) formatDate(dateInput) else "",
                        onValueChange = {},
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
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
                        value = gallonsInput,
                        onValueChange = { gallonsInput = it },
                        label = { Text("Gallons") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Total Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val g = gallonsInput.toDoubleOrNull() ?: return@Button
                    val c = costInput.toDoubleOrNull() ?: return@Button
                    if (dateInput.isBlank()) return@Button
                    val existing = selectedRecord
                    if (existing != null) {
                        viewModel.updateRecord(existing.copy(date = dateInput, gallons = g, totalCost = c, notes = notesInput))
                    } else {
                        viewModel.addRecord(dateInput, g, c, notesInput)
                    }
                    showEntryDialog = false
                }) { Text("Save", color = Color.White) }
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
            text = { Text("Are you sure you want to delete this gas record?") },
            confirmButton = {
                Button(
                    onClick = { selectedRecord?.let { viewModel.deleteRecord(it) }; showDeleteDialog = false; selectedRecord = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ServiceStatusItem(status: ServiceStatus, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PurinBrown),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            if (status.mileageProgress >= 0f) {
                LinearProgressIndicator(
                    progress = { status.mileageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        status.mileageProgress >= 0.9f -> Color.Red
                        status.mileageProgress >= 0.75f -> Color(0xFFFF9800)
                        else -> Color.Green
                    },
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mileage", color = Color.White, fontSize = 12.sp)
                    Text(
                        status.mileageText,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            if (status.timeProgress >= 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { status.timeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        status.timeProgress >= 0.9f -> Color.Red
                        status.timeProgress >= 0.75f -> Color(0xFFFF9800)
                        else -> Color.Green
                    },
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time", color = Color.White, fontSize = 12.sp)
                    Text(
                        status.timeText,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

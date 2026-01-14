package com.example.purincar.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purincar.data.CarEntity
import com.example.purincar.ui.theme.PurinBrown
import com.example.purincar.ui.theme.PurinYellow
import com.example.purincar.viewmodels.CarDetailsViewModel
import com.example.purincar.viewmodels.ServiceStatus
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

@Composable
fun CarDetailsScreen(
    viewModel: CarDetailsViewModel,
    onServiceClick: (String) -> Unit
) {
    val car by viewModel.carInfo.collectAsState(initial = null)
    val serviceStatuses by viewModel.serviceStatuses.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PurinYellow,
        bottomBar = {
            NavigationBar(containerColor = PurinBrown) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Status", tint = Color.White) },
                    label = { Text("Status", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Black.copy(alpha = 0.2f))
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Records", tint = Color.White) },
                    label = { Text("Records", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Black.copy(alpha = 0.2f))
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (selectedTab == 0) VehicleStatusTab(car, viewModel) else ServiceRecordsTab(car, serviceStatuses, viewModel, onServiceClick)
        }
    }
}

@Composable
fun CarHeader(car: CarEntity?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(text = car?.name ?: "Loading...", fontSize = 28.sp, color = PurinBrown, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        Text(text = "Mileage: ${car?.currentMileage ?: 0} miles", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun VehicleStatusTab(car: CarEntity?, viewModel: CarDetailsViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarHeader(car)

        Card(colors = CardDefaults.cardColors(containerColor = PurinBrown), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Vehicle Health", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        StatusItem("Range", "${car?.range?.roundToInt() ?: "-"} mi", isRightAligned = false)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        StatusItem("Fuel/EV", "${((car?.fuelPercent ?: 0.0) * 100).toInt()}%", isRightAligned = true)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        StatusItem("Oil Life", "${((car?.oilLife ?: 0.0) * 100).toInt()}%", isRightAligned = false)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        StatusItem("Tires (PSI)", car?.tirePressure ?: "N/A", isRightAligned = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                StatusItem("Doors", if (car?.isLocked == true) "Locked" else "Unlocked")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { car?.smartcarId?.let { id -> viewModel.toggleLock(id, true, context) } },
                colors = ButtonDefaults.buttonColors(containerColor = PurinBrown),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lock")
            }
            Button(
                onClick = { car?.smartcarId?.let { id -> viewModel.toggleLock(id, false, context) } },
                colors = ButtonDefaults.buttonColors(containerColor = PurinBrown),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}

@Composable
fun ServiceRecordsTab(car: CarEntity?, serviceStatuses: List<ServiceStatus>, viewModel: CarDetailsViewModel, onServiceClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { try { context.contentResolver.openInputStream(it)?.use { inputStream -> viewModel.importCsv(BufferedReader(InputStreamReader(inputStream)).readText()) } } catch (e: Exception) { e.printStackTrace() } } }
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? -> uri?.let { scope.launch { val csvData = viewModel.generateCsvExport(); try { context.contentResolver.openOutputStream(it)?.use { outputStream -> outputStream.write(csvData.toByteArray()) } } catch (e: Exception) { e.printStackTrace() } } } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        item { CarHeader(car) }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { importLauncher.launch("*/*") }, colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)) { Text("Import CSV", color = Color.White) }
                    Button(onClick = { exportLauncher.launch("car_records_export.csv") }, colors = ButtonDefaults.buttonColors(containerColor = PurinBrown)) { Text("Export CSV", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = PurinBrown); Spacer(modifier = Modifier.height(16.dp))
            }
        }
        items(serviceStatuses) { status -> ServiceStatusItem(status = status, onClick = { onServiceClick(status.name) }); Spacer(modifier = Modifier.height(16.dp)) }
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

@Composable
fun ServiceStatusItem(status: ServiceStatus, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = PurinBrown), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(status.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            if (status.mileageProgress >= 0f) {
                LinearProgressIndicator(progress = { status.mileageProgress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (status.mileageProgress > 0.9f) Color.Red else Color.Green, trackColor = Color.White.copy(alpha = 0.3f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Mileage", color = Color.White, fontSize = 12.sp); Text(status.mileageText, color = Color.White, fontSize = 12.sp) }
            }
            if (status.timeProgress >= 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { status.timeProgress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (status.timeProgress > 0.9f) Color.Red else Color.Green, trackColor = Color.White.copy(alpha = 0.3f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Time", color = Color.White, fontSize = 12.sp); Text(status.timeText, color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

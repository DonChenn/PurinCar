// The car details page with its Status, Records and Gas tabs.
package com.example.purincar.feature.cardetails

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.R
import com.example.purincar.core.common.AppConfig
import com.example.purincar.core.ui.EmptyPane
import com.example.purincar.core.ui.LoadingPane
import com.example.purincar.core.ui.MessageSnackbar
import com.example.purincar.core.ui.PurinCarTopBar
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.SubduedAlpha
import com.example.purincar.data.car.Car
import com.example.purincar.data.gas.GasRecord
import com.example.purincar.data.maintenance.ServiceType
import com.example.purincar.data.maintenance.serviceStatuses
import com.example.purincar.data.odometer.OdometerReading
import com.example.purincar.feature.smartcar.message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

enum class DetailsTab(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    STATUS(R.string.details_tab_status, Icons.Filled.Info),
    RECORDS(R.string.details_tab_records, Icons.AutoMirrored.Filled.List),
    GAS(R.string.details_tab_gas, Icons.Filled.LocalGasStation)
}

// Car details page connected to its view model, reading and writing CSV files for import and export.
@Composable
fun CarDetailsScreen(
    carId: Long,
    onBack: () -> Unit,
    onOpenService: (ServiceType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarDetailsViewModel = viewModel(factory = CarDetailsViewModel.factory(carId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) viewModel.onCsvFileResult(exported = false) else viewModel.importCsv(text)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(AppConfig.CSV_MIME_TYPE)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = viewModel.exportCsv()
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } != null
                }.getOrDefault(false)
            }
            viewModel.onCsvFileResult(exported = written)
        }
    }

    CarDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onRefreshSmartcar = viewModel::refreshSmartcar,
        onOpenService = onOpenService,
        onImportCsv = { importLauncher.launch(CSV_PICKER_TYPE) },
        onExportCsv = { exportLauncher.launch(AppConfig.CSV_EXPORT_FILE_NAME) },
        onAddFillUp = viewModel::addFillUp,
        onUpdateFillUp = viewModel::updateFillUp,
        onDeleteFillUp = viewModel::deleteFillUp,
        onSmartcarMessageShown = viewModel::onSmartcarMessageShown,
        onCsvMessageShown = viewModel::onCsvMessageShown,
        modifier = modifier
    )
}

private const val CSV_PICKER_TYPE = "*/*"

// Top bar with the car's name, the selected tab's content and the tab bar along the bottom.
@Composable
fun CarDetailsContent(
    uiState: CarDetailsUiState,
    onBack: () -> Unit,
    onRefreshSmartcar: () -> Unit,
    onOpenService: (ServiceType) -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onAddFillUp: (LocalDate, Double, Double, String) -> Unit,
    onUpdateFillUp: (GasRecord) -> Unit,
    onDeleteFillUp: (GasRecord) -> Unit,
    onSmartcarMessageShown: () -> Unit,
    onCsvMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: DetailsTab = DetailsTab.STATUS
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val recordsListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageSnackbar(uiState.smartcarStatus.message(), snackbarHostState, onSmartcarMessageShown)
    MessageSnackbar(uiState.csvMessage?.text(), snackbarHostState, onCsvMessageShown)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PurinCarTopBar(title = uiState.car?.name.orEmpty(), onBack = onBack) },
        bottomBar = { DetailsTabBar(selected = selectedTab, onSelect = { selectedTab = it }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val car = uiState.car
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            !uiState.loaded -> LoadingPane(content)

            car == null -> EmptyPane(stringResource(R.string.details_car_missing), content)

            else -> when (selectedTab) {
                DetailsTab.STATUS -> StatusTab(
                    car = car,
                    odometer = uiState.odometer,
                    refreshing = uiState.refreshing,
                    onRefresh = onRefreshSmartcar,
                    modifier = content
                )

                DetailsTab.RECORDS -> RecordsTab(
                    currentMileage = car.currentMileage,
                    statuses = uiState.statuses,
                    listState = recordsListState,
                    onImportCsv = onImportCsv,
                    onExportCsv = onExportCsv,
                    onOpenService = onOpenService,
                    modifier = content
                )

                DetailsTab.GAS -> GasTab(
                    fillUps = uiState.fillUps,
                    totalSpent = uiState.totalSpent,
                    totalGallons = uiState.totalGallons,
                    onAdd = onAddFillUp,
                    onUpdate = onUpdateFillUp,
                    onDelete = onDeleteFillUp,
                    modifier = content
                )
            }
        }
    }
}

// The brown bar along the bottom for switching between Status, Records and Gas.
@Composable
private fun DetailsTabBar(selected: DetailsTab, onSelect: (DetailsTab) -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = onPrimary
    ) {
        DetailsTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = onPrimary,
                    selectedTextColor = onPrimary,
                    unselectedIconColor = onPrimary.copy(alpha = SubduedAlpha),
                    unselectedTextColor = onPrimary.copy(alpha = SubduedAlpha),
                    indicatorColor = onPrimary.copy(alpha = IndicatorAlpha)
                )
            )
        }
    }
}

private const val IndicatorAlpha = 0.2f

// Returns the snackbar text for a CSV import or export result.
@Composable
private fun CsvMessage.text(): String = when (this) {
    is CsvMessage.Imported -> {
        val total = services + fillUps
        val imported = pluralStringResource(R.plurals.details_csv_imported, total, total)
        if (skipped > 0) {
            imported + " " + pluralStringResource(R.plurals.details_csv_skipped, skipped, skipped)
        } else {
            imported
        }
    }

    CsvMessage.Exported -> stringResource(R.string.details_csv_exported)
    CsvMessage.Failed -> stringResource(R.string.details_csv_failed)
}

internal object CarDetailsPreviewData {
    val car = Car(
        id = 1,
        name = "2019 Toyota Corolla",
        currentMileage = 42_180,
        isSmartcarLinked = true,
        lastSyncedAt = Instant.parse("2026-09-26T15:12:00Z"),
        lastBackgroundCheckAt = Instant.parse("2026-09-27T09:00:00Z")
    )

    val odometer = (0 until 30).map { week ->
        OdometerReading(date = LocalDate.of(2026, 3, 1).plusWeeks(week.toLong()), miles = 36_000 + week * 210)
    }.reversed()

    val fillUps = listOf(
        GasRecord(id = 1, carId = 1, date = LocalDate.of(2026, 9, 20), gallons = 11.204, totalCost = 52.61),
        GasRecord(
            id = 2,
            carId = 1,
            date = LocalDate.of(2026, 9, 6),
            gallons = 10.87,
            totalCost = 49.10,
            notes = "Costco"
        )
    )

    val statuses = serviceStatuses(
        currentMileage = car.currentMileage,
        records = emptyList(),
        today = LocalDate.of(2026, 9, 27)
    )
}

private class CarDetailsStateProvider : PreviewParameterProvider<CarDetailsUiState> {
    override val values = sequenceOf(
        CarDetailsUiState(loaded = false),
        CarDetailsUiState(loaded = true, car = null),
        CarDetailsUiState(
            loaded = true,
            car = CarDetailsPreviewData.car,
            statuses = CarDetailsPreviewData.statuses,
            odometer = CarDetailsPreviewData.odometer,
            fillUps = CarDetailsPreviewData.fillUps
        )
    )
}

@Preview(name = "Car details", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun CarDetailsContentPreview(
    @PreviewParameter(CarDetailsStateProvider::class) uiState: CarDetailsUiState
) {
    PurinCarTheme {
        CarDetailsContent(
            uiState = uiState,
            onBack = {},
            onRefreshSmartcar = {},
            onOpenService = {},
            onImportCsv = {},
            onExportCsv = {},
            onAddFillUp = { _, _, _, _ -> },
            onUpdateFillUp = {},
            onDeleteFillUp = {},
            onSmartcarMessageShown = {},
            onCsvMessageShown = {}
        )
    }
}

@Preview(name = "Records tab", showBackground = true)
@Composable
private fun CarDetailsRecordsPreview() {
    PurinCarTheme {
        CarDetailsContent(
            uiState = CarDetailsUiState(
                loaded = true,
                car = CarDetailsPreviewData.car,
                statuses = CarDetailsPreviewData.statuses
            ),
            onBack = {},
            onRefreshSmartcar = {},
            onOpenService = {},
            onImportCsv = {},
            onExportCsv = {},
            onAddFillUp = { _, _, _, _ -> },
            onUpdateFillUp = {},
            onDeleteFillUp = {},
            onSmartcarMessageShown = {},
            onCsvMessageShown = {},
            initialTab = DetailsTab.RECORDS
        )
    }
}

@Preview(name = "Gas tab", showBackground = true)
@Composable
private fun CarDetailsGasPreview() {
    PurinCarTheme {
        CarDetailsContent(
            uiState = CarDetailsUiState(
                loaded = true,
                car = CarDetailsPreviewData.car,
                fillUps = CarDetailsPreviewData.fillUps
            ),
            onBack = {},
            onRefreshSmartcar = {},
            onOpenService = {},
            onImportCsv = {},
            onExportCsv = {},
            onAddFillUp = { _, _, _, _ -> },
            onUpdateFillUp = {},
            onDeleteFillUp = {},
            onSmartcarMessageShown = {},
            onCsvMessageShown = {},
            initialTab = DetailsTab.GAS
        )
    }
}

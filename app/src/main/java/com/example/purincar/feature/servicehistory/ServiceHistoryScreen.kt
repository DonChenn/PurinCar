// The history page for one kind of service on a car.
package com.example.purincar.feature.servicehistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.R
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.common.toLongDate
import com.example.purincar.core.common.toMiles
import com.example.purincar.core.common.toMoney
import com.example.purincar.core.ui.ConfirmDeleteDialog
import com.example.purincar.core.ui.PurinCarTopBar
import com.example.purincar.core.ui.RecordCard
import com.example.purincar.core.ui.RecordOptionsDialog
import com.example.purincar.core.ui.ResultPane
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.data.maintenance.MaintenanceRecord
import com.example.purincar.data.maintenance.ServiceType
import java.time.LocalDate

private sealed interface RecordDialog {
    data class Options(val record: MaintenanceRecord) : RecordDialog
    data class Entry(val record: MaintenanceRecord?) : RecordDialog
    data class ConfirmDelete(val record: MaintenanceRecord) : RecordDialog
}

// Service history page connected to its view model.
@Composable
fun ServiceHistoryScreen(
    carId: Long,
    serviceType: ServiceType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceHistoryViewModel = viewModel(factory = ServiceHistoryViewModel.factory(carId, serviceType))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ServiceHistoryContent(
        uiState = uiState,
        onBack = onBack,
        onAdd = viewModel::addRecord,
        onUpdate = viewModel::updateRecord,
        onDelete = viewModel::deleteRecord,
        modifier = modifier
    )
}

// Total spent on the service, a card per record that opens edit or delete, and a button to add one.
@Composable
fun ServiceHistoryContent(
    uiState: ServiceHistoryUiState,
    onBack: () -> Unit,
    onAdd: (LocalDate, Int, Double, String) -> Unit,
    onUpdate: (MaintenanceRecord) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var dialog by remember { mutableStateOf<RecordDialog?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PurinCarTopBar(title = stringResource(uiState.serviceType.labelRes), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialog = RecordDialog.Entry(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.service_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.spaceMedium)
            ) {
                Text(
                    text = stringResource(R.string.service_total_cost, uiState.totalCost.toMoney()),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimensions.spaceSmall)
                )
            }
            ResultPane(
                result = uiState.records,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                emptyMessage = stringResource(R.string.service_empty)
            ) { records ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimensions.spaceMedium,
                        end = Dimensions.spaceMedium,
                        bottom = Dimensions.fabClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecordCard(
                            date = record.date.toLongDate(),
                            detail = stringResource(R.string.service_miles_value, record.mileageAtService.toMiles()),
                            amount = record.cost.takeIf { it > 0 }?.toMoney(),
                            notes = record.description,
                            onLongClick = { dialog = RecordDialog.Options(record) }
                        )
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit

        is RecordDialog.Options -> RecordOptionsDialog(
            onEdit = { dialog = RecordDialog.Entry(current.record) },
            onDelete = { dialog = RecordDialog.ConfirmDelete(current.record) },
            onDismiss = { dialog = null }
        )

        is RecordDialog.Entry -> RecordEntryDialog(
            record = current.record,
            currentMileage = uiState.currentMileage,
            onSave = { date, mileage, cost, description ->
                val existing = current.record
                if (existing == null) {
                    onAdd(date, mileage, cost, description)
                } else {
                    onUpdate(existing.copy(date = date, mileageAtService = mileage, cost = cost, description = description))
                }
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is RecordDialog.ConfirmDelete -> ConfirmDeleteDialog(
            title = stringResource(R.string.record_delete_title),
            message = stringResource(R.string.service_delete_message),
            onConfirm = {
                onDelete(current.record)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
    }
}

internal object ServiceHistoryPreviewData {
    val records = listOf(
        MaintenanceRecord(
            id = 1,
            carId = 1,
            serviceType = ServiceType.ENGINE_OIL,
            date = LocalDate.of(2026, 6, 14),
            mileageAtService = 38_020,
            description = "Full synthetic, new filter",
            cost = 64.99
        ),
        MaintenanceRecord(
            id = 2,
            carId = 1,
            serviceType = ServiceType.ENGINE_OIL,
            date = LocalDate.of(2025, 12, 2),
            mileageAtService = 33_150
        )
    )
}

private class ServiceHistoryStateProvider : PreviewParameterProvider<ServiceHistoryUiState> {
    override val values = sequenceOf(
        ServiceHistoryUiState(ServiceType.ENGINE_OIL, records = PurinCarResult.Loading),
        ServiceHistoryUiState(ServiceType.ENGINE_OIL, records = PurinCarResult.Success(emptyList())),
        ServiceHistoryUiState(
            ServiceType.ENGINE_OIL,
            records = PurinCarResult.Success(ServiceHistoryPreviewData.records),
            currentMileage = 42_180
        ),
        ServiceHistoryUiState(ServiceType.ENGINE_OIL, records = PurinCarResult.Error("The database couldn't be read."))
    )
}

@Preview(name = "Service history", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun ServiceHistoryContentPreview(
    @PreviewParameter(ServiceHistoryStateProvider::class) uiState: ServiceHistoryUiState
) {
    PurinCarTheme {
        ServiceHistoryContent(
            uiState = uiState,
            onBack = {},
            onAdd = { _, _, _, _ -> },
            onUpdate = {},
            onDelete = {}
        )
    }
}

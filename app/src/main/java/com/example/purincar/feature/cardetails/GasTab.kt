// The Gas tab: fill-up totals, every fill-up, and adding, editing and deleting them.
package com.example.purincar.feature.cardetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.purincar.R
import com.example.purincar.core.common.toGallons
import com.example.purincar.core.common.toLongDate
import com.example.purincar.core.common.toMoney
import com.example.purincar.core.ui.ConfirmDeleteDialog
import com.example.purincar.core.ui.RecordCard
import com.example.purincar.core.ui.RecordOptionsDialog
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.data.gas.GasRecord
import java.time.LocalDate

private sealed interface GasDialog {
    data class Options(val record: GasRecord) : GasDialog
    data class Entry(val record: GasRecord?) : GasDialog
    data class ConfirmDelete(val record: GasRecord) : GasDialog
}

// Total spent and gallons, a card per fill-up that opens edit or delete, and a button to add one.
@Composable
fun GasTab(
    fillUps: List<GasRecord>,
    totalSpent: Double,
    totalGallons: Double,
    onAdd: (LocalDate, Double, Double, String) -> Unit,
    onUpdate: (GasRecord) -> Unit,
    onDelete: (GasRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var dialog by remember { mutableStateOf<GasDialog?>(null) }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimensions.spaceMedium,
                end = Dimensions.spaceMedium,
                top = Dimensions.spaceMedium,
                bottom = Dimensions.fabClearance
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            item(contentType = HeaderContentType) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.gas_total_spent, totalSpent.toMoney()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.gas_total_gallons, totalGallons.toGallons()),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = Dimensions.spaceSmall)
                    )
                }
            }
            items(fillUps, key = { it.id }, contentType = { FillUpContentType }) { record ->
                RecordCard(
                    date = record.date.toLongDate(),
                    detail = stringResource(R.string.gas_gallons_value, record.gallons.toGallons(decimals = 3)),
                    amount = record.totalCost.toMoney(),
                    notes = record.notes,
                    onLongClick = { dialog = GasDialog.Options(record) }
                )
            }
        }

        FloatingActionButton(
            onClick = { dialog = GasDialog.Entry(null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimensions.spaceMedium)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.gas_add))
        }
    }

    when (val current = dialog) {
        null -> Unit

        is GasDialog.Options -> RecordOptionsDialog(
            onEdit = { dialog = GasDialog.Entry(current.record) },
            onDelete = { dialog = GasDialog.ConfirmDelete(current.record) },
            onDismiss = { dialog = null }
        )

        is GasDialog.Entry -> GasEntryDialog(
            record = current.record,
            onSave = { date, gallons, cost, notes ->
                val existing = current.record
                if (existing == null) {
                    onAdd(date, gallons, cost, notes)
                } else {
                    onUpdate(existing.copy(date = date, gallons = gallons, totalCost = cost, notes = notes))
                }
                dialog = null
            },
            onDismiss = { dialog = null }
        )

        is GasDialog.ConfirmDelete -> ConfirmDeleteDialog(
            title = stringResource(R.string.record_delete_title),
            message = stringResource(R.string.gas_delete_message),
            onConfirm = {
                onDelete(current.record)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
    }
}

private const val HeaderContentType = "header"
private const val FillUpContentType = "fillUp"

@Preview(name = "Gas tab", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun GasTabPreview() {
    PurinCarTheme {
        GasTab(
            fillUps = CarDetailsPreviewData.fillUps,
            totalSpent = CarDetailsPreviewData.fillUps.sumOf { it.totalCost },
            totalGallons = CarDetailsPreviewData.fillUps.sumOf { it.gallons },
            onAdd = { _, _, _, _ -> },
            onUpdate = {},
            onDelete = {}
        )
    }
}

@Preview(name = "No fill-ups", showBackground = true)
@Composable
private fun GasTabEmptyPreview() {
    PurinCarTheme {
        GasTab(
            fillUps = emptyList(),
            totalSpent = 0.0,
            totalGallons = 0.0,
            onAdd = { _, _, _, _ -> },
            onUpdate = {},
            onDelete = {}
        )
    }
}

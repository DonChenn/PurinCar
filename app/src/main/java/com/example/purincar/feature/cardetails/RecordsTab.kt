// The Records tab: CSV import and export and a status card for every service.
package com.example.purincar.feature.cardetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.data.maintenance.ServiceStatus
import com.example.purincar.data.maintenance.ServiceType

// Odometer header, Import and Export CSV buttons, then each service's progress card that opens its history.
@Composable
fun RecordsTab(
    currentMileage: Int,
    statuses: List<ServiceStatus>,
    listState: LazyListState,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenService: (ServiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(Dimensions.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
    ) {
        item(contentType = HeaderContentType) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                OdometerHeader(currentMileage)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)) {
                    Button(onClick = onImportCsv) { Text(stringResource(R.string.details_import_csv)) }
                    Button(onClick = onExportCsv) { Text(stringResource(R.string.details_export_csv)) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)
            }
        }
        items(statuses, key = { it.type }, contentType = { StatusContentType }) { status ->
            ServiceStatusItem(status = status, onClick = { onOpenService(status.type) })
        }
    }
}

private const val HeaderContentType = "header"
private const val StatusContentType = "status"

@Preview(name = "Records tab", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun RecordsTabPreview() {
    PurinCarTheme {
        RecordsTab(
            currentMileage = CarDetailsPreviewData.car.currentMileage,
            statuses = CarDetailsPreviewData.statuses,
            listState = rememberLazyListState(),
            onImportCsv = {},
            onExportCsv = {},
            onOpenService = {}
        )
    }
}

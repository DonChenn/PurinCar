// The Status tab: odometer, Smartcar sync health and the odometer history chart.
package com.example.purincar.feature.cardetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.common.toDateTime
import com.example.purincar.core.common.toMiles
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.subduedColor
import com.example.purincar.data.car.Car
import com.example.purincar.data.odometer.OdometerReading

// Odometer header, a Vehicle Health card with Smartcar refresh and sync times, and the history chart.
@Composable
fun StatusTab(
    car: Car,
    odometer: List<OdometerReading>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.spaceMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
    ) {
        OdometerHeader(car.currentMileage)
        HorizontalDivider(color = MaterialTheme.colorScheme.primary)

        BrownCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.details_vehicle_health), style = MaterialTheme.typography.titleLarge)
                if (car.isSmartcarLinked) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            color = LocalContentColor.current,
                            strokeWidth = Dimensions.spinnerStroke,
                            modifier = Modifier
                                .padding(Dimensions.spaceSmall)
                                .size(Dimensions.refreshSpinnerSize)
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.details_refresh))
                        }
                    }
                }
            }
            car.lastSyncedAt?.let {
                Text(
                    text = stringResource(R.string.details_last_synced, it.toDateTime()),
                    style = MaterialTheme.typography.bodySmall,
                    color = subduedColor()
                )
            }
            car.lastBackgroundCheckAt?.let {
                Text(
                    text = stringResource(R.string.details_last_background_check, it.toDateTime()),
                    style = MaterialTheme.typography.bodySmall,
                    color = subduedColor()
                )
            }
        }

        if (odometer.isNotEmpty()) {
            BrownCard {
                Text(stringResource(R.string.details_odometer_history), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Dimensions.spaceMedium))
                OdometerChart(readings = odometer, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// The car's odometer in large text.
@Composable
fun OdometerHeader(miles: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.details_odometer, miles.toMiles()),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

// Full-width brown card with white text used for the Status tab's sections.
@Composable
private fun BrownCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimensions.spaceMedium)) { content() }
    }
}

@Preview(name = "Status tab", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun StatusTabPreview() {
    PurinCarTheme {
        StatusTab(
            car = CarDetailsPreviewData.car,
            odometer = CarDetailsPreviewData.odometer,
            refreshing = false,
            onRefresh = {}
        )
    }
}

@Preview(name = "Refreshing", showBackground = true)
@Composable
private fun StatusTabRefreshingPreview() {
    PurinCarTheme {
        StatusTab(car = CarDetailsPreviewData.car, odometer = emptyList(), refreshing = true, onRefresh = {})
    }
}

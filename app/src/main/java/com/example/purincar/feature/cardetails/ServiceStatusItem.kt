// The card showing how close one service is to due, by mileage and by time.
package com.example.purincar.feature.cardetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.common.toMiles
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.statusColor
import com.example.purincar.data.maintenance.ServiceStatus
import com.example.purincar.data.maintenance.ServiceType

// Service name with a green, orange or red bar for mileage and for time, opening its history on tap.
@Composable
fun ServiceStatusItem(
    status: ServiceStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            Text(stringResource(status.type.labelRes), style = MaterialTheme.typography.titleLarge)
            status.mileageProgress?.let { progress ->
                ProgressRow(
                    label = stringResource(R.string.service_mileage),
                    value = stringResource(
                        R.string.service_mileage_value,
                        status.milesDriven.toMiles(),
                        status.type.mileageInterval.toMiles()
                    ),
                    progress = progress
                )
            }
            status.timeProgress?.let { progress ->
                ProgressRow(
                    label = stringResource(R.string.service_time),
                    value = pluralStringResource(
                        R.plurals.service_time_value,
                        status.type.dayInterval,
                        status.daysElapsed,
                        status.type.dayInterval
                    ),
                    progress = progress
                )
            }
        }
    }
}

// A colored progress bar with its label and value underneath.
@Composable
private fun ProgressRow(label: String, value: String, progress: Float) {
    Column {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = statusColor(progress),
            trackColor = LocalContentColor.current.copy(alpha = TrackAlpha),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.progressHeight)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private const val TrackAlpha = 0.3f

@Preview(name = "Service status", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun ServiceStatusItemPreview() {
    PurinCarTheme {
        ServiceStatusItem(
            status = ServiceStatus(
                type = ServiceType.ENGINE_OIL,
                milesDriven = 4_100,
                daysElapsed = 95,
                mileageProgress = 0.82f,
                timeProgress = 0.53f
            ),
            onClick = {}
        )
    }
}

@Preview(name = "No intervals", showBackground = true)
@Composable
private fun ServiceStatusItemMiscPreview() {
    PurinCarTheme {
        ServiceStatusItem(
            status = ServiceStatus(ServiceType.MISCELLANEOUS, 0, 0, mileageProgress = null, timeProgress = null),
            onClick = {}
        )
    }
}

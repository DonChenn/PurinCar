// The brown card each service and gas record is listed in.
package com.example.purincar.core.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.subduedColor

// Date and amount on top, a bold detail line, and optional italic notes, opening options on long press.
@Composable
fun RecordCard(
    date: String,
    detail: String,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    amount: String? = null,
    notes: String = ""
) {
    val optionsLabel = stringResource(R.string.record_options_title)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = optionsLabel,
                onLongClickLabel = optionsLabel,
                onClick = onLongClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(Dimensions.spaceMedium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, style = MaterialTheme.typography.bodyLarge)
                amount?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            }
            Text(detail, style = MaterialTheme.typography.titleMedium)
            if (notes.isNotBlank()) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = subduedColor(),
                    modifier = Modifier.padding(top = Dimensions.spaceXSmall)
                )
            }
        }
    }
}

@Preview(name = "Record card", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun RecordCardPreview() {
    PurinCarTheme {
        RecordCard(
            date = "March 4, 2026",
            detail = "42,180 miles",
            amount = "$64.99",
            notes = "Full synthetic, new filter",
            onLongClick = {}
        )
    }
}

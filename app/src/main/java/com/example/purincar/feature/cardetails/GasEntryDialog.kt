// The form for adding or editing a fill-up.
package com.example.purincar.feature.cardetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.CurrencyField
import com.example.purincar.core.ui.DatePickerField
import com.example.purincar.core.ui.centsPrefill
import com.example.purincar.core.ui.currencyFieldValue
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.data.gas.GasRecord
import java.time.LocalDate

// Date, gallons, total cost and notes, prefilled when editing, with Save enabled once the required fields are valid.
@Composable
fun GasEntryDialog(
    record: GasRecord?,
    onSave: (date: LocalDate, gallons: Double, totalCost: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var date by rememberSaveable { mutableStateOf(record?.date) }
    var gallons by rememberSaveable { mutableStateOf(record?.gallons?.toString().orEmpty()) }
    var cost by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(currencyFieldValue(centsPrefill(record?.totalCost ?: 0.0)))
    }
    var notes by rememberSaveable { mutableStateOf(record?.notes.orEmpty()) }

    val parsedGallons = gallons.toDoubleOrNull()
    val parsedCost = cost.text.toDoubleOrNull()
    val pickedDate = date

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (record == null) R.string.gas_add_title else R.string.gas_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                DatePickerField(date = date, onDateChange = { date = it }, label = stringResource(R.string.form_date))
                OutlinedTextField(
                    value = gallons,
                    onValueChange = { gallons = it },
                    label = { Text(stringResource(R.string.gas_gallons)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CurrencyField(value = cost, onValueChange = { cost = it }, label = stringResource(R.string.gas_total_cost))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.gas_notes)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pickedDate != null && parsedGallons != null && parsedCost != null) {
                        onSave(pickedDate, parsedGallons, parsedCost, notes)
                    }
                },
                enabled = pickedDate != null && parsedGallons != null && parsedCost != null
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(name = "Add fill-up")
@Composable
private fun GasEntryDialogAddPreview() {
    PurinCarTheme { GasEntryDialog(record = null, onSave = { _, _, _, _ -> }, onDismiss = {}) }
}

@Preview(name = "Edit fill-up")
@Composable
private fun GasEntryDialogEditPreview() {
    PurinCarTheme {
        GasEntryDialog(record = CarDetailsPreviewData.fillUps.last(), onSave = { _, _, _, _ -> }, onDismiss = {})
    }
}

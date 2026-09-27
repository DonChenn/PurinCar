// The form for adding or editing a service record.
package com.example.purincar.feature.servicehistory

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
import com.example.purincar.data.maintenance.MaintenanceRecord
import java.time.LocalDate

// Date, mileage, cost and description, prefilled when editing, with Save enabled once a date and mileage are set.
@Composable
fun RecordEntryDialog(
    record: MaintenanceRecord?,
    currentMileage: Int,
    onSave: (date: LocalDate, mileage: Int, cost: Double, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var date by rememberSaveable { mutableStateOf(record?.date) }
    var mileage by rememberSaveable { mutableStateOf(record?.mileageAtService?.toString().orEmpty()) }
    var cost by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(currencyFieldValue(centsPrefill(record?.cost ?: 0.0)))
    }
    var description by rememberSaveable { mutableStateOf(record?.description.orEmpty()) }

    val parsedMileage = mileage.toIntOrNull()
    val pickedDate = date

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (record == null) R.string.service_add_title else R.string.service_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
                DatePickerField(date = date, onDateChange = { date = it }, label = stringResource(R.string.form_date))
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { mileage = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.service_mileage_field)) },
                    placeholder = { Text(currentMileage.toString()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CurrencyField(value = cost, onValueChange = { cost = it }, label = stringResource(R.string.service_cost))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.service_description)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pickedDate != null && parsedMileage != null) {
                        onSave(pickedDate, parsedMileage, cost.text.toDoubleOrNull() ?: 0.0, description)
                    }
                },
                enabled = pickedDate != null && parsedMileage != null
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(name = "Add record")
@Composable
private fun RecordEntryDialogAddPreview() {
    PurinCarTheme { RecordEntryDialog(record = null, currentMileage = 42_180, onSave = { _, _, _, _ -> }, onDismiss = {}) }
}

@Preview(name = "Edit record")
@Composable
private fun RecordEntryDialogEditPreview() {
    PurinCarTheme {
        RecordEntryDialog(
            record = ServiceHistoryPreviewData.records.first(),
            currentMileage = 42_180,
            onSave = { _, _, _, _ -> },
            onDismiss = {}
        )
    }
}

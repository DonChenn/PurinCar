// Date and money fields used by the add and edit record forms.
package com.example.purincar.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.common.toLongDate
import com.example.purincar.core.ui.theme.PurinCarTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

// Read-only field showing the picked date that opens a calendar when tapped.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) picking = true }
    }

    OutlinedTextField(
        value = date?.toLongDate().orEmpty(),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = {
            IconButton(onClick = { picking = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.form_pick_date))
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).toUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateChange(it.toUtcDate()) }
                        picking = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

// Converts a calendar date to the UTC midnight the date picker works in.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

// Converts the date picker's UTC midnight back to a calendar date.
private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

// Money field that fills from the cents place as digits are typed, so 1, 2, 3 reads 0.01, 0.12, 1.23.
@Composable
fun CurrencyField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(currencyFieldValue(formatCentsInput(it.text))) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

// Formats typed digits as a cents-first amount, dropping anything that isn't a digit.
fun formatCentsInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.trimStart('0').take(MaxCentsDigits)
    if (digits.isEmpty()) return ""
    return String.format(Locale.US, "%.2f", digits.toLong() / 100.0)
}

private const val MaxCentsDigits = 9

// Pre-fills a money field from a saved amount, leaving it blank for zero.
fun centsPrefill(amount: Double): String =
    if (amount > 0) String.format(Locale.US, "%.2f", amount) else ""

// Wraps money text with the cursor pinned to the end so new digits always append.
fun currencyFieldValue(text: String): TextFieldValue =
    TextFieldValue(text, selection = TextRange(text.length))

@Preview(name = "Form fields", showBackground = true)
@Composable
private fun FormFieldsPreview() {
    PurinCarTheme {
        Column {
            DatePickerField(date = LocalDate.of(2026, 3, 4), onDateChange = {}, label = "Date")
            CurrencyField(value = currencyFieldValue("45.10"), onValueChange = {}, label = "Total Cost ($)")
        }
    }
}

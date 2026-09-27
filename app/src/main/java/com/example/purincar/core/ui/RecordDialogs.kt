// The edit-or-delete, confirm-delete and plain confirmation dialogs shared across pages.
package com.example.purincar.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.theme.PurinCarTheme

// Asks whether to edit or delete the record that was long-pressed.
@Composable
fun RecordOptionsDialog(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_options_title)) },
        text = { Text(stringResource(R.string.record_options_message)) },
        confirmButton = {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

// Makes sure the user really wants to delete something before it goes.
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// Asks the user to confirm an action with a labelled confirm button and Cancel.
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Preview(name = "Options")
@Composable
private fun RecordOptionsDialogPreview() {
    PurinCarTheme { RecordOptionsDialog(onEdit = {}, onDelete = {}, onDismiss = {}) }
}

@Preview(name = "Confirm delete")
@Composable
private fun ConfirmDeleteDialogPreview() {
    PurinCarTheme {
        ConfirmDeleteDialog(
            title = "Delete Record?",
            message = "Are you sure you want to delete this gas record?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Confirm")
@Composable
private fun ConfirmDialogPreview() {
    PurinCarTheme {
        ConfirmDialog(
            title = "Sign out?",
            message = "Your garage stays saved to your account.",
            confirmLabel = "Sign out",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

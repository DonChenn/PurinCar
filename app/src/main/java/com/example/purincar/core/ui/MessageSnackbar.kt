// Shows one-off messages as snackbars and reports back once each has been seen.
package com.example.purincar.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

// Shows the message in the snackbar host whenever it changes, then calls onShown.
@Composable
fun MessageSnackbar(message: String?, snackbarHostState: SnackbarHostState, onShown: () -> Unit) {
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            shown()
        }
    }
}

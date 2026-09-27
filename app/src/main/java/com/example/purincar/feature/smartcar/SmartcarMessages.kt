// The words shown for each Smartcar connection status.
package com.example.purincar.feature.smartcar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.purincar.R
import com.example.purincar.data.smartcar.SmartcarStatus

// Returns the snackbar text for a finished Smartcar status, or null while idle or syncing.
@Composable
fun SmartcarStatus.message(): String? = when (this) {
    SmartcarStatus.Idle, SmartcarStatus.Syncing -> null
    is SmartcarStatus.Synced -> stringResource(R.string.smartcar_updated, carName)
    SmartcarStatus.NotConnected -> stringResource(R.string.smartcar_not_connected)
    SmartcarStatus.LoginCancelled -> stringResource(R.string.smartcar_login_cancelled)
    is SmartcarStatus.Failed -> stringResource(R.string.smartcar_failed, message)
}

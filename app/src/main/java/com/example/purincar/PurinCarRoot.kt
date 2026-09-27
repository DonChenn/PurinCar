// Top-level composable: the sign-in page or the signed-in app, plus the notification permission prompt.
package com.example.purincar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.feature.navigation.PurinCarNavDisplay
import com.example.purincar.feature.navigation.RootViewModel
import com.example.purincar.feature.signin.SignInScreen

// Shows sign-in until someone is signed in, then the garage and everything under it.
@Composable
fun PurinCarRoot(
    modifier: Modifier = Modifier,
    viewModel: RootViewModel = viewModel(factory = RootViewModel.Factory)
) {
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()

    RequestNotificationPermission()

    if (signedIn) {
        PurinCarNavDisplay(modifier = modifier)
    } else {
        SignInScreen(modifier = modifier)
    }
}

// Asks once for permission to post service reminders on Android 13 and up.
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

// The garage page listing every car.
package com.example.purincar.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.R
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.ui.ConfirmDeleteDialog
import com.example.purincar.core.ui.ConfirmDialog
import com.example.purincar.core.ui.MessageSnackbar
import com.example.purincar.core.ui.OverflowMenu
import com.example.purincar.core.ui.PurinCarTopBar
import com.example.purincar.core.ui.ResultPane
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.data.car.Car
import com.example.purincar.data.smartcar.SmartcarStatus
import com.example.purincar.feature.smartcar.message

// Garage page connected to its view model.
@Composable
fun GarageScreen(
    onOpenCar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GarageViewModel = viewModel(factory = GarageViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    GarageContent(
        uiState = uiState,
        onOpenCar = onOpenCar,
        onConnectSmartcar = { viewModel.connectSmartcar(context) },
        onDeleteCar = viewModel::deleteCar,
        onMessageShown = viewModel::onMessageShown,
        onSignOut = viewModel::signOut,
        modifier = modifier
    )
}

// Car cards with a button to connect a car through Smartcar, a sign-out menu and a confirmation before deleting.
@Composable
fun GarageContent(
    uiState: GarageUiState,
    onOpenCar: (Long) -> Unit,
    onConnectSmartcar: () -> Unit,
    onDeleteCar: (Long) -> Unit,
    onMessageShown: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var deleting by remember { mutableStateOf<Car?>(null) }
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    MessageSnackbar(uiState.smartcarStatus.message(), snackbarHostState, onMessageShown)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PurinCarTopBar(
                title = stringResource(R.string.garage_title),
                actions = {
                    OverflowMenu(listOf(stringResource(R.string.action_sign_out) to { confirmingSignOut = true }))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!uiState.connecting) onConnectSmartcar() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (uiState.connecting) {
                    CircularProgressIndicator(
                        color = LocalContentColor.current,
                        strokeWidth = Dimensions.spinnerStroke,
                        modifier = Modifier.size(Dimensions.buttonSpinnerSize)
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.garage_connect))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ResultPane(
            result = uiState.cars,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            emptyMessage = stringResource(R.string.garage_empty)
        ) { cars ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimensions.spaceMedium,
                    end = Dimensions.spaceMedium,
                    top = Dimensions.spaceMedium,
                    bottom = Dimensions.fabClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
            ) {
                items(cars, key = { it.id }) { car ->
                    CarCard(
                        name = car.name,
                        onClick = { onOpenCar(car.id) },
                        onLongClick = { deleting = car }
                    )
                }
            }
        }
    }

    deleting?.let { car ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.garage_delete_title),
            message = stringResource(R.string.garage_delete_message, car.name),
            onConfirm = {
                onDeleteCar(car.id)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }

    if (confirmingSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.sign_out_title),
            message = stringResource(R.string.sign_out_message),
            confirmLabel = stringResource(R.string.action_sign_out),
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false }
        )
    }
}

private class GarageStateProvider : PreviewParameterProvider<GarageUiState> {
    private val cars = listOf(
        Car(1, "2019 Toyota Corolla", 42_180, isSmartcarLinked = true, lastSyncedAt = null, lastBackgroundCheckAt = null),
        Car(2, "2015 Honda Civic", 98_004, isSmartcarLinked = false, lastSyncedAt = null, lastBackgroundCheckAt = null)
    )

    override val values = sequenceOf(
        GarageUiState(cars = PurinCarResult.Loading),
        GarageUiState(cars = PurinCarResult.Success(emptyList())),
        GarageUiState(cars = PurinCarResult.Success(cars)),
        GarageUiState(cars = PurinCarResult.Success(cars), smartcarStatus = SmartcarStatus.Syncing)
    )
}

@Preview(name = "Garage", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun GarageContentPreview(
    @PreviewParameter(GarageStateProvider::class) uiState: GarageUiState
) {
    PurinCarTheme {
        GarageContent(
            uiState = uiState,
            onOpenCar = {},
            onConnectSmartcar = {},
            onDeleteCar = {},
            onMessageShown = {},
            onSignOut = {}
        )
    }
}

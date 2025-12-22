package com.example.purincar.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.purincar.data.CarDao
import com.example.purincar.screens.CarDetailsScreen
import com.example.purincar.screens.CarSelectionScreen
import com.example.purincar.viewmodels.CarDetailsViewModel
import com.example.purincar.viewmodels.CarViewModel

@Composable
fun NavigationRoot(dao: CarDao) {
    val backStack = rememberNavBackStack(Route.CarSelectionScreen)

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is Route.CarSelectionScreen -> {
                    NavEntry(key) {
                        // Create ViewModel with Factory to inject DAO
                        val viewModel = viewModel<CarViewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return CarViewModel(dao) as T
                                }
                            }
                        )

                        CarSelectionScreen(
                            viewModel = viewModel,
                            onCarClick = { carObject ->
                                backStack.add(Route.CarDetailsScreen(car = carObject))
                            }
                        )
                    }
                }
                is Route.CarDetailsScreen -> {
                    NavEntry(key) {
                        // Create ViewModel with Factory to inject DAO & Car ID
                        val viewModel = viewModel<CarDetailsViewModel>(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return CarDetailsViewModel(dao, key.car.id) as T
                                }
                            }
                        )

                        CarDetailsScreen(
                            viewModel = viewModel
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}

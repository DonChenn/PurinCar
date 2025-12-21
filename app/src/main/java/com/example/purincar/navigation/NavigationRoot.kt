package com.example.purincar.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.purincar.screens.CarDetailsScreen
import com.example.purincar.screens.CarSelectionScreen

@Composable
fun NavigationRoot() {
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
                        CarSelectionScreen(
                            onCarClick = { carObject ->
                                backStack.add(Route.CarDetailsScreen(car = carObject))
                            }
                        )
                    }
                }
                is Route.CarDetailsScreen -> {
                    NavEntry(key) {
                        CarDetailsScreen(
                            car = key.car
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}
// Maps each route to its page, sliding pages in and out and scoping each page's view model to its entry.
package com.example.purincar.feature.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.purincar.Route
import com.example.purincar.feature.cardetails.CarDetailsScreen
import com.example.purincar.feature.garage.GarageScreen
import com.example.purincar.feature.servicehistory.ServiceHistoryScreen

// The signed-in app: the garage and the pages opened from it.
@Composable
fun PurinCarNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Route.Garage)
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        entryProvider = entryProvider {
            entry<Route.Garage> {
                GarageScreen(onOpenCar = { carId -> backStack.add(Route.CarDetails(carId)) })
            }
            entry<Route.CarDetails> { key ->
                CarDetailsScreen(
                    carId = key.carId,
                    onBack = goBack,
                    onOpenService = { type -> backStack.add(Route.ServiceHistory(key.carId, type)) }
                )
            }
            entry<Route.ServiceHistory> { key ->
                ServiceHistoryScreen(carId = key.carId, serviceType = key.serviceType, onBack = goBack)
            }
        }
    )
}

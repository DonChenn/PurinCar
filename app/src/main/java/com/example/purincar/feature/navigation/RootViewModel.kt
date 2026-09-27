// Holds whether someone is signed in, which decides between the sign-in page and the app.
package com.example.purincar.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.purincar.PurinCarApp
import com.example.purincar.data.auth.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RootViewModel(auth: AuthRepository) : ViewModel() {

    val signedIn: StateFlow<Boolean> = auth.userId
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), auth.currentUserId() != null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as? PurinCarApp
                    ?: error(
                        "Application is not PurinCarApp. Add " +
                            "android:name=\".PurinCarApp\" to <application> in the manifest."
                    )
                RootViewModel(app.container.auth)
            }
        }
    }
}

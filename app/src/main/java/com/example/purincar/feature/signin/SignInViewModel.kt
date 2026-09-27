// Holds whether Google sign-in is in progress and any error it hit.
package com.example.purincar.feature.signin

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.purincar.PurinCarApp
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SignInUiState(
    val signingIn: Boolean = false,
    val error: String? = null
)

class SignInViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    // Opens the Google account picker and signs in with the chosen account.
    fun signIn(activityContext: Context) {
        if (_uiState.value.signingIn) return
        _uiState.update { it.copy(signingIn = true, error = null) }
        viewModelScope.launch {
            val result = auth.signIn(activityContext)
            _uiState.update {
                it.copy(signingIn = false, error = (result as? PurinCarResult.Error)?.message)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as? PurinCarApp
                    ?: error(
                        "Application is not PurinCarApp. Add " +
                            "android:name=\".PurinCarApp\" to <application> in the manifest."
                    )
                SignInViewModel(app.container.auth)
            }
        }
    }
}

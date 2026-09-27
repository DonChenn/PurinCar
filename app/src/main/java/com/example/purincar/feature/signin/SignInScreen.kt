// The sign-in page.
package com.example.purincar.feature.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purincar.R
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.subduedColor

// Sign-in page connected to its view model.
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(factory = SignInViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SignInContent(
        uiState = uiState,
        onSignIn = { viewModel.signIn(context) },
        modifier = modifier
    )
}

// App name, tagline and the Sign in with Google button, with a spinner while signing in and any error below.
@Composable
fun SignInContent(
    uiState: SignInUiState,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Dimensions.spaceXLarge)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Dimensions.spaceSmall))
            Text(
                text = stringResource(R.string.sign_in_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = subduedColor(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimensions.spaceXXLarge))
            Button(
                onClick = onSignIn,
                enabled = !uiState.signingIn,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight)
            ) {
                if (uiState.signingIn) {
                    CircularProgressIndicator(
                        strokeWidth = Dimensions.spinnerStroke,
                        modifier = Modifier.size(Dimensions.buttonSpinnerSize)
                    )
                } else {
                    Text(stringResource(R.string.sign_in_google), style = MaterialTheme.typography.titleMedium)
                }
            }
            uiState.error?.let {
                Spacer(Modifier.height(Dimensions.spaceMedium))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private class SignInStateProvider : PreviewParameterProvider<SignInUiState> {
    override val values = sequenceOf(
        SignInUiState(),
        SignInUiState(signingIn = true),
        SignInUiState(error = "Can't reach the server. Check your internet connection.")
    )
}

@Preview(name = "Sign in", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun SignInContentPreview(
    @PreviewParameter(SignInStateProvider::class) uiState: SignInUiState
) {
    PurinCarTheme {
        SignInContent(uiState = uiState, onSignIn = {})
    }
}

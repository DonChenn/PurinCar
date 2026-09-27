// Shared loading, error and empty placeholders.
package com.example.purincar.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.purincar.R
import com.example.purincar.core.common.PurinCarResult
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.subduedColor

// Centered spinner shown while something loads.
@Composable
fun LoadingPane(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.state_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Warning icon and message with an optional Retry button.
@Composable
fun ErrorPane(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.spaceLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimensions.errorIconSize)
            )
            Spacer(Modifier.height(Dimensions.spaceMedium))
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)

            if (onRetry != null) {
                Spacer(Modifier.height(Dimensions.spaceMedium))
                Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}

// Faded centered message for when there's nothing to show.
@Composable
fun EmptyPane(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.spaceLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = subduedColor(),
            textAlign = TextAlign.Center
        )
    }
}

// Shows the loading spinner, the error with an optional Retry, an optional empty message, or the loaded content.
@Composable
fun <T> ResultPane(
    result: PurinCarResult<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String? = null,
    isEmpty: (T) -> Boolean = { (it as? Collection<*>)?.isEmpty() == true },
    content: @Composable BoxScope.(T) -> Unit
) {
    Box(modifier) {
        when (result) {
            is PurinCarResult.Loading -> LoadingPane()

            is PurinCarResult.Error -> ErrorPane(result.message, onRetry = onRetry)

            is PurinCarResult.Success -> if (emptyMessage != null && isEmpty(result.data)) {
                EmptyPane(emptyMessage)
            } else {
                content(result.data)
            }
        }
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun LoadingPanePreview() {
    PurinCarTheme { LoadingPane() }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun ErrorPanePreview() {
    PurinCarTheme { ErrorPane(message = "Can't reach the server. Check your internet connection.", onRetry = {}) }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun EmptyPanePreview() {
    PurinCarTheme { EmptyPane(message = stringResource(R.string.garage_empty)) }
}

private class ResultPaneStateProvider : PreviewParameterProvider<PurinCarResult<List<String>>> {
    override val values = sequenceOf(
        PurinCarResult.Loading,
        PurinCarResult.Error("Can't reach the server. Check your internet connection."),
        PurinCarResult.Success(emptyList()),
        PurinCarResult.Success(listOf("2019 Toyota Corolla"))
    )
}

@Preview(name = "Result", showBackground = true, widthDp = 400, heightDp = 300)
@Composable
private fun ResultPanePreview(
    @PreviewParameter(ResultPaneStateProvider::class) result: PurinCarResult<List<String>>
) {
    PurinCarTheme {
        ResultPane(
            result = result,
            modifier = Modifier.fillMaxSize(),
            onRetry = {},
            emptyMessage = stringResource(R.string.garage_empty)
        ) { items ->
            Text(items.joinToString(), modifier = Modifier.align(Alignment.Center))
        }
    }
}

// The brown app bar at the top of every page.
package com.example.purincar.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.theme.PurinCarTheme

// Page title with an optional back arrow and trailing action buttons.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurinCarTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// Three-dot button that opens a menu of labelled actions.
@Composable
fun OverflowMenu(items: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.action_more)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (label, onClick) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onClick()
                    }
                )
            }
        }
    }
}

@Preview(name = "Top level", showBackground = true)
@Composable
private fun PurinCarTopBarPreview() {
    PurinCarTheme {
        PurinCarTopBar(title = "My Garage", actions = { OverflowMenu(listOf("Sign out" to {})) })
    }
}

@Preview(name = "With back", showBackground = true)
@Composable
private fun PurinCarTopBarBackPreview() {
    PurinCarTheme { PurinCarTopBar(title = "2019 Toyota Corolla", onBack = {}) }
}

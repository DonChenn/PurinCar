// The big brown card each car is listed on in the garage.
package com.example.purincar.feature.garage

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme

// A car's name on a card that opens it on tap and offers to delete it on long press.
@Composable
fun CarCard(
    name: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.cardElevation),
        modifier = modifier
            .fillMaxWidth()
            .height(Dimensions.carCardHeight)
            .combinedClickable(
                onLongClickLabel = stringResource(R.string.garage_delete_title),
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.spaceMedium),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
    }
}

@Preview(name = "Car card", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun CarCardPreview() {
    PurinCarTheme { CarCard(name = "2019 Toyota Corolla", onClick = {}, onLongClick = {}) }
}

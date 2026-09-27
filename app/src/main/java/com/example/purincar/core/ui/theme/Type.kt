// The app's text styles.
package com.example.purincar.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Default = Typography()

val PurinCarTypography = Typography(
    displaySmall = Default.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.Bold)
)

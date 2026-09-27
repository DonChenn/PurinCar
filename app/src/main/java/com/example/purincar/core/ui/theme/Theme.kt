// The app's Material theme.
package com.example.purincar.core.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val PurinCarColors = lightColorScheme(
    primary = PurinBrown,
    onPrimary = OnPurinBrown,
    primaryContainer = BrownContainer,
    onPrimaryContainer = OnBrownContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = PurinBrown,
    onTertiary = OnPurinBrown,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = PurinYellow,
    onBackground = OnPurinYellow,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Color.Black
)

const val SubduedAlpha = 0.7f

// Faded version of the current text color for labels and secondary text.
@Composable
@ReadOnlyComposable
fun subduedColor(): Color = LocalContentColor.current.copy(alpha = SubduedAlpha)

// Picks green, orange or red for how far through its service interval something is.
fun statusColor(progress: Float): Color = when {
    progress >= DueThreshold -> StatusDue
    progress >= SoonThreshold -> StatusSoon
    else -> StatusGood
}

const val SoonThreshold = 0.75f
const val DueThreshold = 0.9f

// Applies the PurinCar colors and text styles to everything inside it.
@Composable
fun PurinCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PurinCarColors,
        typography = PurinCarTypography,
        content = content
    )
}

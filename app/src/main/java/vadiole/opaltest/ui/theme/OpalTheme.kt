package vadiole.opaltest.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object OpalTheme {
    val colors = DarkOpalColors
    val typography = DefaultOpalTypography
    val spacing = OpalSpacing
    val shapes = OpalShapes
    val iconSize = OpalIconSize
    val layout = OpalLayout
    val alpha = OpalAlpha
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpalTheme(content: @Composable () -> Unit) {
    val colors = OpalTheme.colors
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = colors.accentStart,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.content,
            surface = colors.background,
            onSurface = colors.content
        )
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.content,
            LocalTextStyle provides OpalTheme.typography.callout,
            LocalRippleConfiguration provides OpalRippleConfiguration,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val OpalRippleConfiguration = RippleConfiguration(rippleAlpha = OpalAlpha.ripple)

package vadiole.opaltest.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
class OpalColors(
    val background: Color,
    val content: Color,
    val contentSecondary: Color,
    val fill: Color,
    val fillSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val onAccent: Color,
    val onAccentSecondary: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val glow: Color
) {
    val accent: Brush = Brush.verticalGradient(listOf(accentStart, accentEnd))
}

val DarkOpalColors = OpalColors(
    background = Color(0xFF000000),
    content = Color(0xFFFFFFFF),
    contentSecondary = Color(0x99FFFFFF),
    fill = Color(0x1AFFFFFF),
    fillSubtle = Color(0x0DFFFFFF),
    border = Color(0x1AFFFFFF),
    borderSubtle = Color(0x0DFFFFFF),
    onAccent = Color(0xFF000000),
    onAccentSecondary = Color(0x99000000),
    accentStart = Color(0xFFA4C8FF),
    accentEnd = Color(0xFF9EF9FF),
    glow = Color(0xFF99E2FF)
)

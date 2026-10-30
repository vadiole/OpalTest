package vadiole.opaltest.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Immutable
class OpalColors(
    val background: Color,
    val content: Color,
    val contentHigh: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
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
    val accent: Brush = AngledLinearGradient(listOf(accentStart, accentEnd), angleDegrees = 187.86f)
}

@Immutable
private class AngledLinearGradient(private val colors: List<Color>, private val angleDegrees: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2
        val center = size.center
        return LinearGradientShader(
            from = Offset(center.x - dx * half, center.y - dy * half),
            to = Offset(center.x + dx * half, center.y + dy * half),
            colors = colors
        )
    }

    override fun equals(other: Any?) =
        other is AngledLinearGradient && other.colors == colors && other.angleDegrees == angleDegrees

    override fun hashCode() = 31 * colors.hashCode() + angleDegrees.hashCode()
}

val DarkOpalColors = OpalColors(
    background = Color(0xFF000000),
    content = Color(0xFFFFFFFF),
    contentHigh = Color(0xE6FFFFFF),
    contentSecondary = Color(0x99FFFFFF),
    contentTertiary = Color(0x66FFFFFF),
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

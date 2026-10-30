package vadiole.opaltest.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.ui.theme.OpalTheme

private const val GLOW_EXTENT = 0.644f
private const val GLOW_STEPS = 4

private val glowStops = Array(GLOW_STEPS + 1) { step ->
    val fraction = step.toFloat() / GLOW_STEPS
    fraction to lerp(OpalTheme.colors.glow, Color.Black, fraction).copy(alpha = 1f - fraction)
}

@Composable
fun OpalGlow(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .height(168.dp)
            .clipToBounds()
    ) {
        val center = Offset(size.width / 2, (-6.72).dp.toPx())
        val radiusY = 174.72.dp.toPx()
        val radiusX = 738.34.dp.toPx()
        scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(*glowStops, center = center, radius = radiusY * GLOW_EXTENT),
                radius = radiusY * GLOW_EXTENT,
                center = center,
                alpha = 0.5f
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 402)
@Composable
private fun OpalGlowPreview() {
    OpalTheme {
        OpalGlow(Modifier.fillMaxWidth())
    }
}

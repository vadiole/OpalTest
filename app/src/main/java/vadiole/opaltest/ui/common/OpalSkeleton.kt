package vadiole.opaltest.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vadiole.opaltest.ui.theme.OpalTheme

private const val SHIMMER_DURATION = 1500

@Composable
fun OpalSkeleton(description: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress"
    )
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .shimmer(progress, OpalTheme.colors.content)
    ) {
        content()
    }
}

private fun Modifier.shimmer(progress: State<Float>, highlight: Color): Modifier = drawWithCache {
    val band = 120.dp.toPx()
    val brush = Brush.linearGradient(
        colors = listOf(highlight.copy(alpha = 0f), highlight, highlight.copy(alpha = 0f)),
        start = Offset(-band / 2, -band / 2),
        end = Offset(band / 2, band / 2)
    )
    val travel = size.width + size.height + band * 2
    val layer = Paint().apply { blendMode = BlendMode.Plus }
    onDrawWithContent {
        drawContent()
        val shift = (progress.value * travel - band) / 2
        drawIntoCanvas { it.saveLayer(size.toRect(), layer) }
        drawContent()
        translate(shift, shift) {
            drawRect(brush, topLeft = Offset(-shift, -shift), size = size, blendMode = BlendMode.SrcIn)
        }
        drawIntoCanvas { it.restore() }
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = OpalTheme.shapes.small) {
    Box(
        modifier
            .clip(shape)
            .background(OpalTheme.colors.fill)
    )
}

@Composable
fun SkeletonLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val (lineHeight, glyphHeight) = with(LocalDensity.current) { style.lineHeight.toDp() to style.fontSize.toDp() }
    Box(contentAlignment = Alignment.CenterStart, modifier = modifier.height(lineHeight)) {
        SkeletonBlock(Modifier.size(width, glyphHeight * 0.75f), OpalTheme.shapes.pill)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalSkeletonPreview() {
    OpalTheme {
        OpalSkeleton(description = "Loading", modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)) {
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium)) {
                SkeletonBlock(Modifier.size(100.dp), OpalTheme.shapes.medium)
                Column(Modifier.width(200.dp)) {
                    SkeletonLine(OpalTheme.typography.body, 160.dp)
                    SkeletonLine(OpalTheme.typography.caption, 120.dp)
                    SkeletonBlock(Modifier.fillMaxWidth().height(40.dp), OpalTheme.shapes.pill)
                }
            }
        }
    }
}

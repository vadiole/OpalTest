package vadiole.opaltest.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = OpalTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )
    Canvas(
        modifier = modifier
            .height(6.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) }
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = colors.fill, cornerRadius = radius)
        if (animated > 0f) {
            val width = (size.width * animated).coerceAtLeast(size.height)
            drawRoundRect(brush = colors.accent, size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalProgressBarPreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            listOf(0f, 0.05f, 0.55f, 1f).forEach { OpalProgressBar(progress = it, modifier = Modifier.width(179.dp)) }
        }
    }
}

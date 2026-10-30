package vadiole.opaltest.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalTopBlur(state: HazeState, visibility: () -> Float, height: Dp, modifier: Modifier = Modifier) {
    val currentVisibility by rememberUpdatedState(visibility)
    val visible by remember { derivedStateOf { currentVisibility() > 0f } }
    val scrim = OpalTheme.colors.background
    val scrimAlpha = OpalTheme.alpha.scrim
    val blurRadius = OpalTheme.layout.topBlurRadius
    val blurStyle = remember(blurRadius, visible) {
        HazeBlurStyle {
            blurRadius(blurRadius)
            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
            alpha(if (visible) 1f else 0f)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val brush = Brush.verticalGradient(listOf(scrim.copy(alpha = scrimAlpha), scrim.copy(alpha = 0f)))
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, alpha = currentVisibility())
                }
            }
            .graphicsLayer { alpha = currentVisibility() }
            .hazeBlur(input = HazeInput.Sources(state), style = blurStyle)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .height(height)
    )
}

@Composable
fun rememberTopBlurVisibility(listState: LazyListState): State<Float> {
    val distance = with(LocalDensity.current) { OpalTheme.layout.topBlurFadeDistance.toPx() }
    return remember(listState, distance) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / distance).coerceIn(0f, 1f)
            }
        }
    }
}

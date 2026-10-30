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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
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
    val alpha = visibility()
    val scrim = OpalTheme.colors.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (alpha > 0f) {
                    Modifier
                        .drawWithCache {
                            val brush = Brush.verticalGradient(
                                listOf(scrim.copy(alpha = OpalTheme.alpha.scrim), scrim.copy(alpha = 0f))
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush, alpha = alpha)
                            }
                        }
                        .hazeBlur(
                            input = HazeInput.Sources(state),
                            style = HazeBlurStyle {
                                blurRadius(OpalTheme.layout.topBlurRadius)
                                progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
                                alpha(alpha)
                            }
                        )
                } else {
                    Modifier
                }
            )
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

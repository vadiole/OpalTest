package vadiole.opaltest.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.PathEasing
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

object OpalMotion {
    val Emphasized: Easing = PathEasing(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        }
    )
    val BackGesture: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
    val BackSettle: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Decelerate: Easing = Easing { 1f - (1f - it) * (1f - it) }

    const val SCREEN_DURATION = 450
    const val BACK_CANCEL_DURATION = 200
    const val SCREEN_FADE_IN_DELAY = 50
    const val SCREEN_FADE_IN_DURATION = 83
    const val BACK_COMMIT_TOP_FADE = 0.2f
    const val BACK_COMMIT_SCRIM_FADE = 0.5f
    const val BACK_MIN_SCALE = 0.9f
    const val BACK_SCRIM_ALPHA = 77f / 255f

    val ScreenOffset = 96.dp
    val BackEdgeMargin = 16.dp
    val FallbackDisplayCornerRadius = 24.dp
}

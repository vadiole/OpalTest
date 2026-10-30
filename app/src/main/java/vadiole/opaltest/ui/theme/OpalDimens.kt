package vadiole.opaltest.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.ui.unit.dp

object OpalSpacing {
    val xxxSmall = 2.dp
    val xxSmall = 4.dp
    val xSmall = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val mediumLarge = 20.dp
    val large = 24.dp
    val xLarge = 32.dp
}

object OpalShapes {
    val small = SquircleShape(12.dp)
    val medium = SquircleShape(16.dp)
    val large = SquircleShape(24.dp)
    val xLarge = SquircleShape(28.dp)
    val pill = CircleShape
}

object OpalIconSize {
    val small = 20.dp
    val medium = 24.dp
}

object OpalAlpha {
    val disabled = 0.4f
    val completed = 0.75f
    val scrim = 0.6f
    val ripple = RippleAlpha(draggedAlpha = 0.08f, focusedAlpha = 0.1f, hoveredAlpha = 0.05f, pressedAlpha = 0.1f)
    val rippleOnFill =
        RippleAlpha(draggedAlpha = 0.12f, focusedAlpha = 0.12f, hoveredAlpha = 0.06f, pressedAlpha = 0.14f)
    val rippleOnLight =
        RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0.16f, hoveredAlpha = 0.08f, pressedAlpha = 0.22f)
}

object OpalLayout {
    val maxContentWidth = 480.dp
    val controlHeight = 50.dp
    val controlHeightSmall = 40.dp
    val iconButtonSize = 44.dp
    val topBlurRadius = 16.dp
    val topBlurFadeDistance = 32.dp
}

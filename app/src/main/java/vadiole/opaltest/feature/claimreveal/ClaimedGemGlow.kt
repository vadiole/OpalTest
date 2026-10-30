package vadiole.opaltest.feature.claimreveal

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

@Composable
fun ClaimedGemGlow(@DrawableRes gem: Int, visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) CLAIMED_GLOW_ALPHA else 0f,
        animationSpec = tween(CLAIMED_GLOW_MS, easing = StrongEaseOut)
    )
    Image(
        painter = painterResource(gem),
        contentDescription = null,
        colorFilter = GlowSaturation,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            val radius = size.minDimension * CLAIMED_GLOW_BLUR
            renderEffect = BlurEffect(radius, radius, TileMode.Decal)
        }
    )
}

private const val CLAIMED_GLOW_ALPHA = 0.7f
private const val CLAIMED_GLOW_BLUR = 0.12f
private const val CLAIMED_GLOW_MS = 700

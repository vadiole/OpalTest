package vadiole.opaltest.feature.claimreveal

import android.graphics.ImageDecoder
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ClaimedGemGlow(@DrawableRes gem: Int, visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) CLAIMED_GLOW_ALPHA else 0f,
        animationSpec = tween(CLAIMED_GLOW_MS, easing = StrongEaseOut)
    )
    val bitmap = rememberDecodedBitmap(gem).value ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,
        colorFilter = GlowSaturation,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            val radius = size.minDimension * CLAIMED_GLOW_BLUR
            renderEffect = BlurEffect(radius, radius, TileMode.Decal)
        }
    )
}

@Composable
internal fun rememberDecodedBitmap(@DrawableRes id: Int, mipmap: Boolean = false): State<ImageBitmap?> {
    val resources = LocalResources.current
    return produceState<ImageBitmap?>(null, id, resources) {
        value = withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(resources, id)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
                .also { it.setHasMipMap(mipmap) }
                .asImageBitmap()
        }
    }
}

private const val CLAIMED_GLOW_ALPHA = 0.7f
private const val CLAIMED_GLOW_BLUR = 0.12f
private const val CLAIMED_GLOW_MS = 700

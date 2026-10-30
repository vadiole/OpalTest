package vadiole.opaltest.ui.common

import android.content.res.Resources
import android.graphics.ImageDecoder
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vadiole.opaltest.ui.theme.OpalMotion

private val bitmaps = LruCache<Long, ImageBitmap>(16)

private fun Resources.key(id: Int) = (configuration.densityDpi.toLong() shl 32) or id.toLong()

private fun Resources.decode(id: Int): ImageBitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(this, id)).asImageBitmap().also { bitmaps.put(key(id), it) }

@Composable
fun OpalImage(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val resources = LocalResources.current
    val sync = LocalInspectionMode.current
    val cached = remember(id, resources) { bitmaps[resources.key(id)] ?: if (sync) resources.decode(id) else null }
    val bitmap by produceState(cached, id, resources) {
        value = cached ?: withContext(Dispatchers.IO) { resources.decode(id) }
    }
    val alpha = remember(id, resources) { Animatable(if (cached != null) 1f else 0f) }
    LaunchedEffect(bitmap) {
        if (bitmap != null) alpha.animateTo(1f, OpalMotion.FadeIn)
    }
    Box(modifier) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { this.alpha = alpha.value }
            )
        }
    }
}

package vadiole.opaltest.ui.common

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.ImageDecoder
import android.util.Log
import android.util.TypedValue
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import vadiole.opaltest.ui.theme.OpalMotion

internal data class ImageKey(val path: String, val assetCookie: Int, val densityDpi: Int)

internal sealed interface ImageSource {
    data class Bitmap(val id: Int, val key: ImageKey) : ImageSource
    data class Vector(val id: Int) : ImageSource
}

internal fun Resources.imageSource(@DrawableRes id: Int): ImageSource? {
    val value = TypedValue()
    val path = try {
        getValue(id, value, true)
        value.string?.toString()
    } catch (e: Resources.NotFoundException) {
        Log.w(TAG, "Missing image resource $id", e)
        null
    } ?: return null
    return if (path.endsWith(".xml")) {
        ImageSource.Vector(id)
    } else {
        ImageSource.Bitmap(id, ImageKey(path, value.assetCookie, displayMetrics.densityDpi))
    }
}

private object OpalImageCache {
    private val trimRegistered = AtomicBoolean()

    val bitmaps = DecodeCache<ImageKey, ImageBitmap>(
        maxSize = (Runtime.getRuntime().maxMemory() / 8).toInt(),
        measure = { it.asAndroidBitmap().allocationByteCount },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onError = { key, e -> Log.w(TAG, "Failed to decode ${key.path}", e) }
    )

    fun registerTrim(context: Context) {
        if (!trimRegistered.compareAndSet(false, true)) return
        context.applicationContext.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) = bitmaps.trim(level)

                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = bitmaps.trim(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            }
        )
    }
}

private fun Resources.decode(id: Int): ImageBitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(this, id)).asImageBitmap()

@Composable
fun OpalImage(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    when (val source = remember(id, resources, configuration) { resources.imageSource(id) }) {
        is ImageSource.Bitmap -> BitmapImage(source, resources, contentDescription, contentScale, modifier)

        is ImageSource.Vector -> Image(
            painter = painterResource(source.id),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )

        null -> Box(modifier)
    }
}

@Composable
private fun BitmapImage(
    source: ImageSource.Bitmap,
    resources: Resources,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sync = LocalInspectionMode.current
    val cache = OpalImageCache.bitmaps
    val cached = remember(source) {
        cache[source.key] ?: if (sync) cache.getBlocking(source.key) { resources.decode(source.id) } else null
    }
    val bitmap by produceState(cached, source) {
        OpalImageCache.registerTrim(context)
        value = cached ?: cache.getOrDecode(source.key) { resources.decode(source.id) }
    }
    val alpha = remember(source) { Animatable(if (cached != null) 1f else 0f) }
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

private const val TAG = "OpalImage"

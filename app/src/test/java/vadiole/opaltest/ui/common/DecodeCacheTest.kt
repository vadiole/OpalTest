package vadiole.opaltest.ui.common

import android.content.ComponentCallbacks2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DecodeCacheTest {
    private class Image(val bytes: Int)

    private fun TestScope.cache(maxSize: Int = 100, errors: MutableList<Throwable> = mutableListOf()) =
        DecodeCache<String, Image>(
            maxSize = maxSize,
            measure = { it.bytes },
            scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
            onError = { _, e -> errors += e }
        )

    @Test
    fun concurrentRequestsDecodeOnce() = runTest {
        val cache = cache()
        var decodes = 0
        val decode = { Image(10).also { decodes++ } }
        val first = async { cache.getOrDecode("a", decode) }
        val second = async { cache.getOrDecode("a", decode) }
        advanceUntilIdle()
        assertEquals(1, decodes)
        assertSame(first.await(), second.await())
        assertSame(first.await(), cache["a"])
    }

    @Test
    fun decodeFailureReturnsNullAndRetries() = runTest {
        val errors = mutableListOf<Throwable>()
        val cache = cache(errors = errors)
        assertNull(cache.getOrDecode("a") { throw IllegalStateException("corrupt") })
        assertNull(cache.getOrDecode("b") { throw OutOfMemoryError() })
        assertNull(cache.getBlocking("c") { throw IllegalStateException("corrupt") })
        assertEquals(3, errors.size)
        val image = Image(10)
        assertSame(image, cache.getOrDecode("a") { image })
    }

    @Test
    fun evictsByBytes() = runTest {
        val cache = cache(maxSize = 100)
        cache.getOrDecode("a") { Image(60) }
        cache.getOrDecode("b") { Image(30) }
        cache.getOrDecode("c") { Image(30) }
        assertNull(cache["a"])
        assertEquals(30, cache["b"]?.bytes)
        assertEquals(30, cache["c"]?.bytes)
    }

    @Test
    fun trimsOnMemoryPressure() = runTest {
        val cache = cache(maxSize = 100)
        cache.getOrDecode("a") { Image(40) }
        cache.getOrDecode("b") { Image(40) }
        cache.trim(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertNull(cache["a"])
        assertEquals(40, cache["b"]?.bytes)
        cache.trim(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertNull(cache["b"])
    }

    @Test
    fun removeDropsCachedAndInFlightDecodes() = runTest {
        val cache = cache()
        cache.getOrDecode("a") { Image(10) }
        cache.remove("a")
        assertNull(cache["a"])
        val evictedMidDecode = cache.getOrDecode("b") {
            cache.remove("b")
            Image(10)
        }
        assertEquals(10, evictedMidDecode?.bytes)
        assertNull(cache["b"])
        assertEquals(20, cache.getOrDecode("b") { Image(20) }?.bytes)
        assertEquals(20, cache["b"]?.bytes)
    }
}

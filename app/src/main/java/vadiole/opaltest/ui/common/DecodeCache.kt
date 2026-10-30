package vadiole.opaltest.ui.common

import android.content.ComponentCallbacks2
import android.util.LruCache
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.job

internal class DecodeCache<K : Any, V : Any>(
    maxSize: Int,
    measure: (V) -> Int,
    private val scope: CoroutineScope,
    private val onError: (K, Throwable) -> Unit = { _, _ -> }
) {
    private val cache = object : LruCache<K, V>(maxSize) {
        override fun sizeOf(key: K, value: V): Int = measure(value).coerceAtLeast(1)
    }
    private val inFlight = HashMap<K, Deferred<V>>()

    operator fun get(key: K): V? = cache.get(key)

    fun getBlocking(key: K, decode: () -> V): V? = cache.get(key) ?: try {
        decode().also { cache.put(key, it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        onError(key, e)
        null
    }

    suspend fun getOrDecode(key: K, decode: () -> V): V? {
        val deferred = synchronized(inFlight) {
            cache.get(key)?.let { return it }
            inFlight.getOrPut(key) {
                scope.async(start = CoroutineStart.LAZY) {
                    val self = coroutineContext.job
                    try {
                        decode().also { synchronized(inFlight) { if (inFlight[key] === self) cache.put(key, it) } }
                    } finally {
                        synchronized(inFlight) { if (inFlight[key] === self) inFlight.remove(key) }
                    }
                }
            }
        }
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(key, e)
            null
        }
    }

    fun remove(key: K) {
        synchronized(inFlight) {
            inFlight.remove(key)
            cache.remove(key)
        }
    }

    fun trim(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(cache.maxSize() / 2)
        }
    }
}

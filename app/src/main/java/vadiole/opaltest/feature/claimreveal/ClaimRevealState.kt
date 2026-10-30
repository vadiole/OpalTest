package vadiole.opaltest.feature.claimreveal

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RevealContent(@DrawableRes val gem: Int, val title: String)

internal enum class RevealPhase { Opening, Revealed, Closing }

@Stable
class ClaimRevealState internal constructor(private val scope: CoroutineScope) {
    var activeKey: Any? by mutableStateOf(null)
        private set
    internal var content: RevealContent? by mutableStateOf(null)
        private set
    internal var phase by mutableStateOf(RevealPhase.Opening)
        private set
    var timeScale by mutableFloatStateOf(1f)

    internal val flight = Animatable(0f, visibilityThreshold = 0.0005f)
    internal val scrim = Animatable(0f)
    internal val items = List(ITEM_STARTS_MS.size) { Animatable(0f) }
    internal val back = Animatable(0f)

    internal var target: Rect? by mutableStateOf(null)
    internal var host: LayoutCoordinates? = null
    private val sources = mutableMapOf<Any, LayoutCoordinates>()
    private val lastSourceRects = mutableMapOf<Any, Rect>()
    private var job: Job? = null
    private var runScale = 1f

    fun isHolding(key: Any): Boolean = activeKey == key

    internal fun isFlying(key: Any): Boolean = activeKey == key && target != null

    fun open(key: Any, content: RevealContent) {
        if (activeKey != null) return
        activeKey = key
        this.content = content
        phase = RevealPhase.Opening
        run {
            launch { scrim.animateTo(1f, tween(520, easing = StrongEaseOut)) }
            delay(scaled(200))
            launch { flight.animateTo(1f, FlightOutSpring) }
            snapshotFlow { flight.value }.first { it >= SETTLE_THRESHOLD }
            phase = RevealPhase.Revealed
            delay(scaled(TEXT_DELAY_MS))
            items.forEachIndexed { index, item ->
                launch {
                    delay(scaled(ITEM_STARTS_MS[index]))
                    item.animateTo(1f, tween(ITEM_ENTER_MS, easing = EnterEase))
                }
            }
        }
    }

    fun close() {
        if (activeKey == null || phase == RevealPhase.Closing) return
        phase = RevealPhase.Closing
        val velocity = flight.velocity
        run {
            items.forEach { launch { it.animateTo(0f, tween(ITEM_EXIT_MS, easing = StrongEaseOut)) } }
            launch {
                delay(scaled(120))
                scrim.animateTo(0f, tween(520, easing = StrongEaseInOut))
            }
            delay(scaled(90))
            val landing = launch { flight.animateTo(0f, FlightBackSpring, initialVelocity = velocity) }
            snapshotFlow { flight.value }.first { abs(it) <= ARRIVAL_THRESHOLD }
            landing.cancel()
            finish()
        }
    }

    internal fun onBackProgress(progress: Float) {
        scope.launch { back.snapTo(progress) }
    }

    internal fun onBackCommitted() {
        val shrink = displayedFlight()
        val fade = 1f - back.value
        scope.launch {
            flight.snapTo(shrink)
            items.forEach { it.snapTo(it.value * fade) }
            back.snapTo(0f)
            close()
        }
    }

    internal fun onBackCancelled() {
        scope.launch { back.animateTo(0f, spring(dampingRatio = 1f, stiffness = 400f)) }
    }

    internal fun displayedFlight(): Float = flight.value * (1f - BACK_SHRINK * back.value)

    internal fun registerSource(key: Any, coordinates: LayoutCoordinates) {
        sources[key] = coordinates
    }

    internal fun sourceRect(key: Any): Rect? {
        val hostCoordinates = host?.takeIf { it.isAttached } ?: return lastSourceRects[key]
        val source = sources[key]?.takeIf { it.isAttached } ?: return lastSourceRects[key]
        return hostCoordinates.localBoundingBoxOf(source, clipBounds = false).also { lastSourceRects[key] = it }
    }

    private suspend fun finish() {
        flight.snapTo(0f)
        scrim.snapTo(0f)
        items.forEach { it.snapTo(0f) }
        back.snapTo(0f)
        activeKey = null
        content = null
        target = null
    }

    private fun run(block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        val systemScale = scope.coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        runScale = timeScale * systemScale
        job = scope.launch(TimeScale(runScale)) { coroutineScope(block) }
    }

    private fun scaled(millis: Int): Long = (millis * runScale).toLong()

    private class TimeScale(override val scaleFactor: Float) : MotionDurationScale
}

@Composable
fun rememberClaimRevealState(): ClaimRevealState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ClaimRevealState(scope) }
}

internal val StrongEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
internal val EnterEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
internal val StrongEaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

private val FlightOutSpring = spring(dampingRatio = 0.86f, stiffness = 60f, visibilityThreshold = 0.0005f)
private val FlightBackSpring = spring(dampingRatio = 0.92f, stiffness = 140f, visibilityThreshold = 0.0005f)

private const val SETTLE_THRESHOLD = 0.96f
private const val ARRIVAL_THRESHOLD = 0.003f
private const val BACK_SHRINK = 0.18f
private val ITEM_STARTS_MS = intArrayOf(0, 320, 1200)
private const val TEXT_DELAY_MS = 550
private const val ITEM_ENTER_MS = 600
private const val ITEM_EXIT_MS = 180

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
    internal var landed by mutableStateOf(false)
        private set
    internal var reducedMotion by mutableStateOf(false)
        private set
    var timeScale by mutableFloatStateOf(1f)
    var sound by mutableStateOf(RevealSound.Shine1)

    internal val flight = Animatable(0f, visibilityThreshold = 0.0005f)
    internal val scrim = Animatable(0f)
    internal val glow = Animatable(0f)
    internal val charge = Animatable(0f)
    internal val sweep = Animatable(0f)
    internal val fade = Animatable(1f)
    internal val items = List(ITEM_STARTS_MS.size) { Animatable(0f) }
    internal val back = Animatable(0f)

    internal var target: Rect? by mutableStateOf(null)
    internal var host: LayoutCoordinates? = null
    internal var feedback: RevealFeedback? = null
    private val sources = mutableMapOf<Any, LayoutCoordinates>()
    private val lastSourceRects = mutableMapOf<Any, Rect>()
    private var job: Job? = null
    private var runScale = 1f

    fun isHolding(key: Any): Boolean = activeKey == key && !landed

    internal fun isFlying(key: Any): Boolean = isHolding(key) && target != null

    fun open(key: Any, content: RevealContent) {
        if (activeKey != null && !landed) return
        activeKey = key
        this.content = content
        replay()
    }

    fun replay() {
        if (activeKey == null) return
        feedback?.claim()
        run {
            reset()
            if (reducedMotion) openReduced() else openFull()
        }
    }

    fun close() {
        if (activeKey == null || phase == RevealPhase.Closing) return
        phase = RevealPhase.Closing
        run {
            coroutineScope {
                items.forEach { launch { it.animateTo(0f, tween(ITEM_EXIT_MS, easing = StrongEaseOut)) } }
                if (reducedMotion) closeReduced() else closeFull()
            }
            finish()
        }
    }

    private suspend fun CoroutineScope.openFull() {
        launch { scrim.animateTo(1f, tween(SCRIM_IN_MS, easing = StrongEaseOut)) }
        launch {
            charge.animateTo(1f, tween(CHARGE_IN_MS, easing = EnterEase))
            charge.animateTo(0f, tween(CHARGE_OUT_MS, easing = StrongEaseInOut))
        }
        launch {
            delay(scaled(GLOW_DELAY_MS))
            glow.animateTo(1f, tween(GLOW_IN_MS, easing = StrongEaseOut))
        }
        delay(scaled(FLIGHT_DELAY_MS))
        launch { flight.animateTo(1f, FlightOutSpring) }
        val ticks = travelTicks(toward = 1f)
        snapshotFlow { flight.value }.first { it >= SWEEP_THRESHOLD }
        ticks.cancel()
        launch { sweep.animateTo(1f, tween(SWEEP_MS, easing = SweepEase)) }
        launch {
            delay((SWEEP_PEAK_MS * runScale - sound.leadMs).toLong().coerceAtLeast(0))
            feedback?.shine(sound)
        }
        launch {
            delay(scaled(SWEEP_PEAK_MS))
            feedback?.peak()
        }
        snapshotFlow { flight.value }.first { it >= SETTLE_THRESHOLD }
        phase = RevealPhase.Revealed
        revealItems()
        repeatSweeps()
    }

    private fun CoroutineScope.repeatSweeps() {
        launch {
            snapshotFlow { items.last().value }.first { it >= 1f }
            delay(scaled(SWEEP_AFTER_BUTTON_MS))
            while (true) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(SWEEP_MS, easing = SweepEase))
                delay(scaled(SWEEP_REPEAT_MS - SWEEP_MS))
            }
        }
    }

    private fun restore(key: Any, content: RevealContent) {
        activeKey = key
        this.content = content
        run {
            reset()
            listOf(flight, scrim, glow).forEach { it.snapTo(1f) }
            items.forEach { it.snapTo(1f) }
            phase = RevealPhase.Revealed
            if (!reducedMotion) repeatSweeps()
        }
    }

    private suspend fun CoroutineScope.openReduced() {
        flight.snapTo(1f)
        fade.snapTo(0f)
        launch { scrim.animateTo(1f, tween(REDUCED_FADE_MS)) }
        launch { glow.animateTo(1f, tween(REDUCED_FADE_MS)) }
        fade.animateTo(1f, tween(REDUCED_FADE_MS))
        feedback?.shine(sound)
        feedback?.peak()
        phase = RevealPhase.Revealed
        revealItems()
    }

    private fun CoroutineScope.revealItems() {
        launch {
            delay(scaled(TEXT_DELAY_MS))
            items.forEachIndexed { index, item ->
                launch {
                    delay(scaled(ITEM_STARTS_MS[index]))
                    item.animateTo(1f, tween(ITEM_ENTER_MS, easing = EnterEase))
                }
            }
        }
    }

    private suspend fun CoroutineScope.closeFull() {
        launch {
            delay(scaled(GLOW_OUT_DELAY_MS))
            glow.animateTo(0f, tween(GLOW_OUT_MS, easing = StrongEaseInOut))
        }
        launch {
            delay(scaled(SCRIM_OUT_DELAY_MS))
            scrim.animateTo(0f, tween(SCRIM_OUT_MS, easing = StrongEaseInOut))
        }
        delay(scaled(FLIGHT_BACK_DELAY_MS))
        val ticks = travelTicks(toward = 0f)
        flight.animateTo(0f, tween(FLIGHT_BACK_MS, easing = StrongEaseInOut))
        ticks.cancel()
        land()
    }

    private suspend fun CoroutineScope.closeReduced() {
        launch { scrim.animateTo(0f, tween(REDUCED_FADE_MS)) }
        launch { glow.animateTo(0f, tween(REDUCED_FADE_MS)) }
        fade.animateTo(0f, tween(REDUCED_FADE_MS))
        land()
    }

    private fun CoroutineScope.travelTicks(toward: Float): Job = launch {
        var last = (flight.value * TRAVEL_TICKS).toInt()
        snapshotFlow { (flight.value * TRAVEL_TICKS).toInt() }.collect { step ->
            if (step == last) return@collect
            last = step
            val closeness = 1f - abs(toward - step / TRAVEL_TICKS.toFloat())
            if (closeness < 1f) feedback?.travel(closeness)
        }
    }

    private suspend fun land() {
        flight.snapTo(0f)
        landed = true
        feedback?.arrive()
    }

    internal fun onBackProgress(progress: Float) {
        scope.launch { back.snapTo(progress) }
    }

    internal fun onBackCommitted() {
        val shrink = displayedFlight()
        val fadeOut = 1f - back.value
        scope.launch {
            flight.snapTo(shrink)
            items.forEach { it.snapTo(it.value * fadeOut) }
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

    private suspend fun reset() {
        listOf(flight, scrim, glow, charge, sweep, back).forEach { it.snapTo(0f) }
        items.forEach { it.snapTo(0f) }
        fade.snapTo(1f)
        landed = false
        phase = RevealPhase.Opening
    }

    private suspend fun finish() {
        reset()
        activeKey = null
        content = null
        target = null
    }

    private fun run(block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        val systemScale = scope.coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        reducedMotion = systemScale == 0f
        runScale = timeScale * if (reducedMotion) 1f else systemScale
        job = scope.launch(TimeScale(runScale)) { coroutineScope(block) }
    }

    private fun scaled(millis: Int): Long = (millis * runScale).toLong()

    private class TimeScale(override val scaleFactor: Float) : MotionDurationScale

    internal companion object {
        fun saver(scope: CoroutineScope) = Saver<ClaimRevealState, List<Any?>>(
            save = { state ->
                val content = state.content?.takeIf { !state.landed && state.phase != RevealPhase.Closing }
                listOf(
                    state.sound.ordinal,
                    state.timeScale,
                    content?.let { state.activeKey },
                    content?.gem,
                    content?.title
                )
            },
            restore = { saved ->
                ClaimRevealState(scope).apply {
                    sound = RevealSound.entries[saved[0] as Int]
                    timeScale = saved[1] as Float
                    val key = saved[2]
                    if (key != null) restore(key, RevealContent(gem = saved[3] as Int, title = saved[4] as String))
                }
            }
        )
    }
}

@Composable
fun rememberClaimRevealState(): ClaimRevealState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = ClaimRevealState.saver(scope)) { ClaimRevealState(scope) }
}

internal val StrongEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
internal val EnterEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
internal val StrongEaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
private val SweepEase = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

private val FlightOutSpring = spring(dampingRatio = 0.86f, stiffness = 60f, visibilityThreshold = 0.0005f)

private const val SCRIM_IN_MS = 520
private const val CHARGE_IN_MS = 380
private const val CHARGE_OUT_MS = 900
private const val GLOW_DELAY_MS = 120
private const val GLOW_IN_MS = 1100
private const val FLIGHT_DELAY_MS = 200
private const val SWEEP_MS = 2800
private const val SWEEP_REPEAT_MS = 10_000
private const val SWEEP_AFTER_BUTTON_MS = 2000
private const val SWEEP_PEAK_MS = SWEEP_MS / 2
private const val GLOW_OUT_DELAY_MS = 90
private const val GLOW_OUT_MS = 900
private const val SCRIM_OUT_DELAY_MS = 120
private const val SCRIM_OUT_MS = 700
private const val FLIGHT_BACK_DELAY_MS = 90
private const val FLIGHT_BACK_MS = 750
private const val REDUCED_FADE_MS = 250
private const val SWEEP_THRESHOLD = 0.88f
private const val TRAVEL_TICKS = 9
private const val SETTLE_THRESHOLD = 0.96f
private const val BACK_SHRINK = 0.18f
private val ITEM_STARTS_MS = intArrayOf(0, 320, 1200)
private const val TEXT_DELAY_MS = 550
private const val ITEM_ENTER_MS = 600
private const val ITEM_EXIT_MS = 180

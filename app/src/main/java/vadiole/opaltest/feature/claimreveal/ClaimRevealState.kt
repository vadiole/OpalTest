package vadiole.opaltest.feature.claimreveal

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class RevealContent(@DrawableRes val gem: Int, @StringRes val title: Int)

internal enum class RevealPhase { Opening, Revealed, Closing, Landed }

internal data class ActiveReveal(val key: Any, val content: RevealContent)

@Stable
class ClaimRevealState internal constructor(private val scope: CoroutineScope) {
    internal var active: ActiveReveal? by mutableStateOf(null)
        private set
    internal val activeKey: Any? get() = active?.key
    internal var phase by mutableStateOf(RevealPhase.Opening)
        private set
    internal val landed: Boolean get() = phase == RevealPhase.Landed
    internal var reducedMotion by mutableStateOf(false)
        private set
    internal var timeScale by mutableFloatStateOf(1f)
    internal var sound: RevealSound? by mutableStateOf(null)
    internal var pathBend by mutableFloatStateOf(DEFAULT_PATH_BEND)
    internal var gemReady by mutableStateOf(false)
    internal var prefetchedGem: Int? by mutableStateOf(null)
        private set

    internal val flight = Animatable(0f, visibilityThreshold = 0.0005f)
    internal val scrim = Animatable(0f)
    internal val glow = Animatable(0f)
    internal val charge = Animatable(0f)
    internal val sweep = Animatable(0f)
    internal val sweepFade = Animatable(1f)
    internal val fade = Animatable(1f)
    internal val items = List(ITEM_STARTS_MS.size) { Animatable(0f) }
    internal val back = Animatable(0f)
    internal val sparkle = Animatable(0f)
    internal val press = Animatable(1f, visibilityThreshold = PRESS_THRESHOLD)

    internal var target: Rect? by mutableStateOf(null)
    internal var host: LayoutCoordinates? = null
    internal var feedback: RevealFeedback? = null
    internal var releaseImage: ((Int) -> Unit)? = null
    private val sources = mutableMapOf<Any, LayoutCoordinates>()
    private val lastSourceRects = mutableMapOf<Any, Rect>()
    private var job: Job? = null
    private var runScale = 1f

    fun isHolding(key: Any): Boolean = activeKey == key && !landed

    internal fun isFlying(key: Any): Boolean = isHolding(key) && target != null && gemReady

    fun prefetch(@DrawableRes gem: Int) {
        prefetchedGem = gem
    }

    fun clearPrefetch() {
        prefetchedGem?.let { releaseImage?.invoke(it) }
        prefetchedGem = null
    }

    fun open(key: Any, content: RevealContent) {
        if (activeKey != null && !landed) return
        active = ActiveReveal(key, content)
        replay()
    }

    internal fun replay() {
        if (activeKey == null) return
        run {
            reset()
            if (reducedMotion) openReduced() else openFull()
        }
    }

    fun close() {
        if (activeKey == null || phase == RevealPhase.Closing || landed) return
        phase = RevealPhase.Closing
        run {
            coroutineScope {
                items.forEach { launch { it.animateTo(0f, tween(ITEM_EXIT_MS, easing = StrongEaseOut)) } }
                launch { charge.animateTo(0f, tween(ITEM_EXIT_MS, easing = StrongEaseOut)) }
                launch { sparkle.animateTo(0f, tween(SPARKLE_OUT_MS, easing = StrongEaseOut)) }
                launch { sweepFade.animateTo(0f, tween(SWEEP_EXIT_MS, easing = StrongEaseOut)) }
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
        awaitGem()
        launch { flight.animateTo(1f, FlightOutSpring) }
        snapshotFlow { flight.value }.first { it >= SWEEP_THRESHOLD }
        launch { sweep.animateTo(1f, tween(SWEEP_MS, easing = SweepEase)) }
        launch {
            val shine = pickSound()
            delay((SWEEP_PEAK_MS * runScale - shine.leadMs).toLong().coerceAtLeast(0))
            feedback?.shine(shine)
        }
        snapshotFlow { flight.value }.first { it >= SETTLE_THRESHOLD }
        phase = RevealPhase.Revealed
        launch { sparkle.animateTo(1f, tween(SPARKLE_IN_MS, easing = EnterEase)) }
        revealItems()
    }

    private fun restore(reveal: ActiveReveal) {
        active = reveal
        run {
            reset()
            listOf(flight, scrim, glow).forEach { it.snapTo(1f) }
            items.forEach { it.snapTo(1f) }
            phase = RevealPhase.Revealed
            if (!reducedMotion) sparkle.snapTo(1f)
        }
    }

    private suspend fun CoroutineScope.openReduced() {
        flight.snapTo(1f)
        fade.snapTo(0f)
        launch { scrim.animateTo(1f, tween(REDUCED_FADE_MS)) }
        launch { glow.animateTo(1f, tween(REDUCED_FADE_MS)) }
        awaitGem()
        fade.animateTo(1f, tween(REDUCED_FADE_MS))
        feedback?.shine(pickSound())
        phase = RevealPhase.Revealed
        revealItems()
    }

    private fun CoroutineScope.revealItems() {
        launch {
            delay(scaled(TEXT_DELAY_MS))
            items.forEachIndexed { index, item ->
                launch {
                    delay(scaled(ITEM_STARTS_MS[index]))
                    if (index == items.lastIndex) feedback?.buttonAppear()
                    item.animateTo(1f, tween(ITEM_ENTER_MS, easing = EnterEase))
                }
            }
        }
    }

    private suspend fun awaitGem() {
        withTimeoutOrNull(GEM_WAIT_MS) { snapshotFlow { gemReady }.first { it } }
    }

    private suspend fun CoroutineScope.closeFull() {
        val key = activeKey
        if (key == null || !hasLiveSource(key)) {
            launch { fade.animateTo(0f, tween(FLIGHT_BACK_DELAY_MS + FLIGHT_BACK_MS, easing = StrongEaseInOut)) }
        }
        launch {
            delay(scaled(GLOW_OUT_DELAY_MS))
            glow.animateTo(0f, tween(GLOW_OUT_MS, easing = StrongEaseInOut))
        }
        launch {
            delay(scaled(SCRIM_OUT_DELAY_MS))
            scrim.animateTo(0f, tween(SCRIM_OUT_MS, easing = StrongEaseInOut))
        }
        delay(scaled(FLIGHT_BACK_DELAY_MS))
        flight.animateTo(0f, tween(FLIGHT_BACK_MS, easing = StrongEaseInOut))
        land()
    }

    private suspend fun CoroutineScope.closeReduced() {
        launch { scrim.animateTo(0f, tween(REDUCED_FADE_MS)) }
        launch { glow.animateTo(0f, tween(REDUCED_FADE_MS)) }
        fade.animateTo(0f, tween(REDUCED_FADE_MS))
        land()
    }

    private fun pickSound(): RevealSound = sound ?: RevealSound.entries.random()

    private suspend fun land() {
        flight.snapTo(0f)
        phase = RevealPhase.Landed
    }

    internal suspend fun onBackProgress(progress: Float) = back.snapTo(progress)

    internal fun onBackCommitted() {
        val shrink = displayedFlight()
        val fadeOut = 1f - back.value
        scope.launch {
            flight.snapTo(shrink)
            items.forEach { it.snapTo(it.value * fadeOut) }
            sparkle.snapTo(sparkle.value * fadeOut)
            back.snapTo(0f)
            close()
        }
    }

    internal fun onBackCancelled() {
        scope.launch { back.animateTo(0f, spring(dampingRatio = 1f, stiffness = 400f)) }
    }

    internal fun pressGem() {
        if (phase != RevealPhase.Revealed || reducedMotion) return
        scope.launch(TimeScale(runScale)) { press.animateTo(PRESSED_SCALE, PressDownSpring) }
    }

    internal fun releaseGem() {
        scope.launch(TimeScale(runScale)) { press.animateTo(1f, PressReleaseSpring) }
    }

    internal fun displayedFlight(): Float = flight.value * (1f - BACK_SHRINK * back.value)

    internal fun registerSource(key: Any, coordinates: LayoutCoordinates) {
        sources[key] = coordinates
    }

    internal fun unregisterSource(key: Any, coordinates: LayoutCoordinates) {
        if (sources[key] !== coordinates) return
        sourceRect(key)
        sources.remove(key)
    }

    private fun hasLiveSource(key: Any): Boolean = host?.isAttached == true && sources[key]?.isAttached == true

    internal fun sourceRect(key: Any): Rect? {
        val hostCoordinates = host?.takeIf { it.isAttached } ?: return lastSourceRects[key]
        val source = sources[key]?.takeIf { it.isAttached } ?: return lastSourceRects[key]
        return hostCoordinates.localBoundingBoxOf(source, clipBounds = false).also { lastSourceRects[key] = it }
    }

    private suspend fun reset() {
        listOf(flight, scrim, glow, charge, sweep, back, sparkle).forEach { it.snapTo(0f) }
        items.forEach { it.snapTo(0f) }
        listOf(fade, sweepFade, press).forEach { it.snapTo(1f) }
        phase = RevealPhase.Opening
    }

    private suspend fun finish() {
        val gem = active?.content?.gem
        gem?.let { releaseImage?.invoke(it) }
        if (prefetchedGem == gem) prefetchedGem = null
        active = null
        target = null
        gemReady = false
        reset()
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
                val active = state.active?.takeIf { state.phase != RevealPhase.Closing && !state.landed }
                listOf(
                    state.sound?.ordinal,
                    state.timeScale,
                    active?.key,
                    active?.content?.gem,
                    active?.content?.title
                )
            },
            restore = { saved ->
                ClaimRevealState(scope).apply {
                    sound = (saved[0] as Int?)?.let { RevealSound.entries[it] }
                    timeScale = saved[1] as Float
                    val key = saved[2] ?: return@apply
                    restore(ActiveReveal(key, RevealContent(gem = saved[3] as Int, title = saved[4] as Int)))
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
private const val PRESSED_SCALE = 0.95f
private const val PRESS_THRESHOLD = 0.0005f
private val PressDownSpring = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = PRESS_THRESHOLD
)
private val PressReleaseSpring = spring(
    dampingRatio = 0.5f,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = PRESS_THRESHOLD
)

private const val SCRIM_IN_MS = 520
private const val CHARGE_IN_MS = 380
private const val CHARGE_OUT_MS = 900
private const val GLOW_DELAY_MS = 120
private const val GLOW_IN_MS = 1100
private const val FLIGHT_DELAY_MS = 200
private const val GEM_WAIT_MS = 1000L
private const val DEFAULT_PATH_BEND = 0.75f
private const val SWEEP_MS = 8400
private const val SWEEP_PEAK_MS = SWEEP_MS / 2
private const val SWEEP_EXIT_MS = 400
private const val GLOW_OUT_DELAY_MS = 90
private const val GLOW_OUT_MS = 900
private const val SCRIM_OUT_DELAY_MS = 120
private const val SCRIM_OUT_MS = 700
private const val FLIGHT_BACK_DELAY_MS = 90
private const val FLIGHT_BACK_MS = 750
private const val REDUCED_FADE_MS = 250
private const val SWEEP_THRESHOLD = 0.88f
private const val SETTLE_THRESHOLD = 0.96f
private const val BACK_SHRINK = 0.18f
private val ITEM_STARTS_MS = intArrayOf(0, 320, 1200)
private const val TEXT_DELAY_MS = 550
private const val ITEM_ENTER_MS = 600
private const val ITEM_EXIT_MS = 180
private const val SPARKLE_IN_MS = 1200
private const val SPARKLE_OUT_MS = 220

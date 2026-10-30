package vadiole.opaltest.navigation

import android.view.RoundedCorner
import android.view.View
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

@Stable
class StackTransitions<T : Any> internal constructor(private val motion: StackMotionState) {

    val isTransitioning: Boolean get() = motion.isTransitioning

    val decorator: NavEntryDecorator<T> = NavEntryDecorator { entry ->
        val keyed = remember(entry) {
            NavEntry<T>(entry) { key: T -> StackEntry(motion, key) { entry.Content() } }
        }
        keyed.Content()
    }

    fun onBackCompleted() = motion.onBackCompleted()

    fun hold(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing), initialAlpha = 1f),
        initialContentExit = fadeOut(tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing), targetAlpha = 1f)
    )
}

@Composable
fun <T : Any> rememberStackTransitions(backStack: List<T>): StackTransitions<T> =
    rememberStackTransitions(backStack, remember { StackMotionState() })

@Composable
internal fun <T : Any> rememberStackTransitions(backStack: List<T>, motion: StackMotionState): StackTransitions<T> {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val stack = backStack.toList()
    SideEffect {
        motion.density = density
        motion.onStack(stack, scope)
    }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    if (dispatcher != null) BlockBackWhileTransitioning(dispatcher, motion)
    val currentBackStack by rememberUpdatedState(backStack)
    LaunchedEffect(motion, dispatcher) {
        val settleScope = this
        dispatcher?.transitionState?.collect { state ->
            when (state) {
                is NavigationEventTransitionState.InProgress -> {
                    if (state.direction == NavigationEventTransitionState.TRANSITIONING_BACK) {
                        motion.onGesture(state.latestEvent, currentBackStack)
                    }
                }

                NavigationEventTransitionState.Idle -> motion.onRelease(settleScope)
            }
        }
    }
    return remember(motion) { StackTransitions(motion) }
}

@Composable
private fun BlockBackWhileTransitioning(dispatcher: NavigationEventDispatcher, motion: StackMotionState) {
    val blocker = remember(motion) { TransitionBackBlocker(motion) }
    DisposableEffect(dispatcher, blocker) {
        dispatcher.addHandler(blocker, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { blocker.remove() }
    }
    LaunchedEffect(blocker) {
        snapshotFlow { motion.isTransitioning }.collect { blocker.isBackEnabled = it }
    }
}

private class TransitionBackBlocker(private val motion: StackMotionState) :
    NavigationEventHandler<NavigationEventInfo>(
        NavigationEventInfo.None,
        isBackEnabled = false,
        isForwardEnabled = false
    ) {

    override fun onBackStarted(event: NavigationEvent) {
        motion.gestureBlocked = true
    }

    override fun onBackCompleted() {
        motion.gestureBlocked = false
    }

    override fun onBackCancelled() {
        motion.gestureBlocked = false
    }
}

@Composable
private fun StackEntry(motion: StackMotionState, key: Any, content: @Composable () -> Unit) {
    LocalNavAnimatedContentScope.current.transition.animateFloat(
        transitionSpec = { tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing) },
        label = "hold"
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    val view = LocalView.current
    val scrim = OpalTheme.colors.background
    var size by remember { mutableStateOf(Size.Zero) }
    val pose by remember(motion, key, view) {
        derivedStateOf { motion.pose(key, size, displayCornerRadius(view, motion.density)) }
    }
    Box(
        propagateMinConstraints = true,
        modifier = Modifier
            .onSizeChanged { size = it.toSize() }
            .graphicsLayer {
                val p = pose
                scaleX = p.scale
                scaleY = p.scale
                translationX = p.translationX
                translationY = p.translationY
                alpha = p.alpha
                if (p.cornerRadius > 0f) {
                    shape = RoundedCornerShape(p.cornerRadius)
                    clip = true
                } else {
                    shape = RectangleShape
                    clip = false
                }
            }
            .drawWithContent {
                drawContent()
                val scrimAlpha = pose.scrimAlpha
                if (scrimAlpha > 0f) drawRect(scrim, alpha = scrimAlpha)
            }
    ) {
        content()
    }
}

private fun displayCornerRadius(view: View, density: Density): Float =
    view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius?.toFloat()
        ?: with(density) { OpalMotion.FallbackDisplayCornerRadius.toPx() }

internal enum class BackPhase { Idle, Gesture, Commit, Cancel }

internal enum class StackMotion { None, Push, Pop }

internal class Pose(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f,
    val cornerRadius: Float = 0f,
    val scrimAlpha: Float = 0f
)

internal val IdentityPose = Pose()

private val HiddenPose = Pose(alpha = 0f)

@Stable
internal class StackMotionState(private val emphasized: Easing = OpalMotion.Emphasized) {
    var density: Density = Density(1f)
    var gestureBlocked = false
    var phase by mutableStateOf(BackPhase.Idle)
        private set
    var motion by mutableStateOf(StackMotion.None)
        private set
    val isTransitioning: Boolean
        get() = motion != StackMotion.None || phase == BackPhase.Commit || phase == BackPhase.Cancel
    private var progress by mutableFloatStateOf(0f)
    private var touchY by mutableFloatStateOf(0f)
    private var topKey: Any? by mutableStateOf(null)
    private var previousKey: Any? by mutableStateOf(null)
    private var startY = 0f
    private var fromLeft = true
    private var completed = false
    private var pendingCommitPop = false
    private var session = 0
    private val settle = Animatable(0f)
    private var settleJob: Job? = null

    private var motionFraction by mutableFloatStateOf(1f)
    private var motionTop: Any? by mutableStateOf(null)
    private var motionPrevious: Any? by mutableStateOf(null)
    private var hiddenKey: Any? by mutableStateOf(null)
    private var motionSession = 0
    private var motionJob: Job? = null
    private var lastStack: List<Any>? = null

    fun onStack(stack: List<Any>, scope: CoroutineScope) {
        val old = lastStack
        lastStack = stack
        if (old == null || old == stack || stack.isEmpty()) return
        if (hiddenKey in stack) hiddenKey = null
        val newTop = stack.last()
        val oldTop = old.lastOrNull()
        if (stack.size < old.size) {
            if (pendingCommitPop) {
                pendingCommitPop = false
                return
            }
            if (oldTop != null && oldTop !in stack) hiddenKey = oldTop
            startMotion(StackMotion.Pop, oldTop, newTop, scope)
        } else if (newTop != oldTop) {
            startMotion(StackMotion.Push, newTop, stack.getOrNull(stack.lastIndex - 1), scope)
        }
    }

    fun onGesture(event: NavigationEvent, backStack: List<Any>) {
        if (gestureBlocked) return
        if (phase != BackPhase.Gesture) {
            if (backStack.size <= 1 || isTransitioning) return
            session++
            completed = false
            pendingCommitPop = false
            topKey = backStack[backStack.lastIndex]
            previousKey = backStack[backStack.lastIndex - 1]
            startY = event.touchY
            fromLeft = event.swipeEdge == NavigationEvent.EDGE_LEFT
            phase = BackPhase.Gesture
        }
        progress = event.progress
        touchY = event.touchY
    }

    fun onBackCompleted() {
        if (phase == BackPhase.Gesture) {
            completed = true
            pendingCommitPop = true
        }
    }

    fun onRelease(scope: CoroutineScope) {
        if (phase != BackPhase.Gesture) return
        val committed = completed
        val releaseSession = session
        settleJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                settle.snapTo(0f)
                if (committed) hiddenKey = topKey
                phase = if (committed) BackPhase.Commit else BackPhase.Cancel
                val spec = if (committed) {
                    tween<Float>(OpalMotion.SCREEN_DURATION, easing = LinearEasing)
                } else {
                    val remaining = (progress * OpalMotion.SCREEN_DURATION).toInt()
                    tween(min(OpalMotion.BACK_CANCEL_DURATION, remaining), easing = OpalMotion.BackSettle)
                }
                settle.animateTo(1f, spec)
            } finally {
                if (session == releaseSession && phase != BackPhase.Gesture) {
                    phase = BackPhase.Idle
                    pendingCommitPop = false
                }
            }
        }
    }

    private fun startMotion(kind: StackMotion, top: Any?, previous: Any?, scope: CoroutineScope) {
        settleJob?.cancel()
        settleJob = null
        phase = BackPhase.Idle
        motionJob?.cancel()
        val id = ++motionSession
        motionTop = top
        motionPrevious = previous
        motionFraction = 0f
        motion = kind
        motionJob = scope.launch {
            try {
                animate(0f, 1f, animationSpec = tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing)) { value, _ ->
                    motionFraction = value
                }
            } finally {
                if (motionSession == id) motion = StackMotion.None
            }
        }
    }

    fun pose(key: Any, size: Size, cornerRadius: Float): Pose {
        if (phase != BackPhase.Idle) {
            if (key == topKey) return backPose(top = true, size, cornerRadius)
            if (key == previousKey) return backPose(top = false, size, cornerRadius)
        }
        if (motion != StackMotion.None) {
            if (key == motionTop) return motionPose(top = true)
            if (key == motionPrevious) return motionPose(top = false)
        }
        return if (key == hiddenKey) HiddenPose else IdentityPose
    }

    private fun motionPose(top: Boolean): Pose {
        val f = motionFraction
        val e = emphasized.transform(f)
        val offset = with(density) { OpalMotion.ScreenOffset.toPx() }
        return when (motion) {
            StackMotion.None -> IdentityPose

            StackMotion.Push -> if (top) {
                Pose(translationX = offset * (1f - e), alpha = fadeGate(f * OpalMotion.SCREEN_DURATION))
            } else {
                Pose(translationX = -offset * e)
            }

            StackMotion.Pop -> if (top) {
                Pose(translationX = offset * e, alpha = 1f - e)
            } else {
                Pose(translationX = -offset * (1f - e))
            }
        }
    }

    private fun backPose(top: Boolean, size: Size, cornerRadius: Float): Pose {
        val w = size.width
        val h = size.height
        val offset = with(density) { OpalMotion.ScreenOffset.toPx() }
        val margin = with(density) { OpalMotion.BackEdgeMargin.toPx() }
        val g = OpalMotion.BackGesture.transform(progress)
        val gestureScale = 1f - (1f - OpalMotion.BACK_MIN_SCALE) * g
        val maxDx = ((w - gestureScale * w) / 2f - margin).coerceAtLeast(0f)
        val gestureTx = if (fromLeft) maxDx * g else 0f
        val deltaY = touchY - startY
        val dyCap = ((h - gestureScale * h) / 2f - margin).coerceAtLeast(0f)
        val yNorm = if (h > 0f) (abs(deltaY) / (h / 2f)).coerceIn(0f, 1f) else 0f
        val gestureTy = sign(deltaY) * dyCap * OpalMotion.Decelerate.transform(yNorm)
        val f = settle.value
        return when (phase) {
            BackPhase.Idle -> IdentityPose

            BackPhase.Gesture -> if (top) {
                Pose(gestureScale, gestureTx, gestureTy, cornerRadius = cornerRadius / gestureScale)
            } else {
                Pose(gestureScale, -offset, gestureTy, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA)
            }

            BackPhase.Commit -> {
                val e = emphasized.transform(f)
                val scale = gestureScale + (1f - gestureScale) * e
                val ty = gestureTy * (1f - e)
                if (top) {
                    val fadeWindow = min(OpalMotion.BACK_COMMIT_TOP_FADE, 1f - progress)
                    val alpha = if (fadeWindow > 0f) (1f - f / fadeWindow).coerceIn(0f, 1f) else 0f
                    Pose(scale, gestureTx + offset * e, ty, alpha, cornerRadius / scale)
                } else {
                    val scrimFade = (1f - f / OpalMotion.BACK_COMMIT_SCRIM_FADE).coerceAtLeast(0f)
                    Pose(scale, -offset * (1f - e), ty, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA * scrimFade)
                }
            }

            BackPhase.Cancel -> {
                val scale = gestureScale + (1f - gestureScale) * f
                val ty = gestureTy * (1f - f)
                if (top) {
                    Pose(scale, gestureTx * (1f - f), ty, cornerRadius = cornerRadius / scale)
                } else {
                    Pose(scale, -offset, ty, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA)
                }
            }
        }
    }

    private fun fadeGate(t: Float): Float =
        ((t - OpalMotion.SCREEN_FADE_IN_DELAY) / OpalMotion.SCREEN_FADE_IN_DURATION).coerceIn(0f, 1f)
}

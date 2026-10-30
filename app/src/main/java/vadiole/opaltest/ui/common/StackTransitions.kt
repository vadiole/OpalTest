package vadiole.opaltest.ui.common

import android.view.RoundedCorner
import android.view.View
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.navigationevent.NavigationEvent
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

@Stable
class StackTransitions<T : Any> internal constructor(private val offsetPx: Int, private val back: PredictiveBackState) {

    val decorator: NavEntryDecorator<T> = NavEntryDecorator { entry ->
        val keyed = remember(entry) {
            NavEntry<T>(entry) { key: T -> PredictiveBackEntry(back, key) { entry.Content() } }
        }
        keyed.Content()
    }

    fun onBackCompleted() = back.onBackCompleted()

    fun push(): ContentTransform = ContentTransform(
        targetContentEnter = slideInHorizontally(screenTween()) { offsetPx } +
            fadeIn(tween(OpalMotion.SCREEN_FADE_IN_DURATION, OpalMotion.SCREEN_FADE_IN_DELAY, LinearEasing)),
        initialContentExit = slideOutHorizontally(screenTween()) { -offsetPx }
    )

    fun pop(): ContentTransform = ContentTransform(
        targetContentEnter = slideInHorizontally(screenTween()) { -offsetPx },
        initialContentExit = slideOutHorizontally(screenTween()) { offsetPx } + fadeOut(screenTween())
    )

    fun predictivePop(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing), initialAlpha = 1f),
        initialContentExit = fadeOut(tween(OpalMotion.SCREEN_DURATION, easing = LinearEasing), targetAlpha = 1f)
    )

    private fun <V> screenTween() = tween<V>(OpalMotion.SCREEN_DURATION, easing = OpalMotion.Emphasized)
}

@Composable
fun <T : Any> rememberStackTransitions(backStack: List<T>): StackTransitions<T> {
    val back = remember { PredictiveBackState() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val currentBackStack by rememberUpdatedState(backStack)
    LaunchedEffect(back, dispatcher) {
        val scope = this
        dispatcher?.transitionState?.collect { state ->
            when (state) {
                is NavigationEventTransitionState.InProgress -> {
                    if (state.direction == NavigationEventTransitionState.TRANSITIONING_BACK) {
                        back.onGesture(state.latestEvent, currentBackStack)
                    }
                }

                NavigationEventTransitionState.Idle -> back.onRelease(scope, currentBackStack.size)
            }
        }
    }
    val stackSize = backStack.size
    LaunchedEffect(back, stackSize) { back.onStackChanged(stackSize) }
    val density = LocalDensity.current
    return remember(back, density) {
        StackTransitions(with(density) { OpalMotion.ScreenOffset.roundToPx() }, back)
    }
}

@Composable
private fun PredictiveBackEntry(back: PredictiveBackState, key: Any, content: @Composable () -> Unit) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scrim = MaterialTheme.colorScheme.scrim
    var size by remember { mutableStateOf(Size.Zero) }
    val pose by remember(back, key, view, density) {
        derivedStateOf { back.pose(back.role(key), size, density, displayCornerRadius(view, density)) }
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

internal enum class BackRole { None, Top, Previous }

internal class Pose(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f,
    val cornerRadius: Float = 0f,
    val scrimAlpha: Float = 0f
)

internal val IdentityPose = Pose()

@Stable
internal class PredictiveBackState {
    var phase by mutableStateOf(BackPhase.Idle)
        private set
    private var session = 0
    private var progress by mutableFloatStateOf(0f)
    private var touchY by mutableFloatStateOf(0f)
    private var topKey: Any? by mutableStateOf(null)
    private var previousKey: Any? by mutableStateOf(null)
    private var startY = 0f
    private var fromLeft = true
    private var completed = false
    private var stackSizeAtRelease = 0
    private val settle = Animatable(0f)
    private var settleJob: Job? = null

    fun role(key: Any): BackRole = when {
        phase == BackPhase.Idle -> BackRole.None
        key == topKey -> BackRole.Top
        key == previousKey -> BackRole.Previous
        else -> BackRole.None
    }

    fun onGesture(event: NavigationEvent, backStack: List<Any>) {
        if (phase != BackPhase.Gesture) {
            if (backStack.size <= 1) return
            settleJob?.cancel()
            settleJob = null
            session++
            completed = false
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
        if (phase == BackPhase.Gesture) completed = true
    }

    fun onRelease(scope: CoroutineScope, stackSize: Int) {
        if (phase != BackPhase.Gesture) return
        val committed = completed
        val releaseSession = session
        stackSizeAtRelease = stackSize
        settleJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                settle.snapTo(0f)
                phase = if (committed) BackPhase.Commit else BackPhase.Cancel
                val spec = if (committed) {
                    tween<Float>(OpalMotion.SCREEN_DURATION, easing = LinearEasing)
                } else {
                    val remaining = (progress * OpalMotion.SCREEN_DURATION).toInt()
                    tween(min(OpalMotion.BACK_CANCEL_DURATION, remaining), easing = OpalMotion.BackSettle)
                }
                settle.animateTo(1f, spec)
            } finally {
                if (session == releaseSession && phase != BackPhase.Gesture) phase = BackPhase.Idle
            }
        }
    }

    fun onStackChanged(stackSize: Int) {
        if ((phase == BackPhase.Commit || phase == BackPhase.Cancel) && stackSize != stackSizeAtRelease) {
            settleJob?.cancel()
            phase = BackPhase.Idle
        }
    }

    fun pose(role: BackRole, size: Size, density: Density, cornerRadius: Float): Pose {
        if (role == BackRole.None) return IdentityPose
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

            BackPhase.Gesture -> when (role) {
                BackRole.Top -> Pose(gestureScale, gestureTx, gestureTy, cornerRadius = cornerRadius / gestureScale)
                else -> Pose(gestureScale, -offset, gestureTy, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA)
            }

            BackPhase.Commit -> {
                val e = OpalMotion.Emphasized.transform(f)
                val scale = gestureScale + (1f - gestureScale) * e
                val ty = gestureTy * (1f - e)
                when (role) {
                    BackRole.Top -> {
                        val fadeWindow = min(OpalMotion.BACK_COMMIT_TOP_FADE, 1f - progress)
                        val alpha = if (fadeWindow > 0f) (1f - f / fadeWindow).coerceIn(0f, 1f) else 0f
                        Pose(scale, gestureTx + offset * e, ty, alpha, cornerRadius / scale)
                    }

                    else -> {
                        val scrimFade = (1f - f / OpalMotion.BACK_COMMIT_SCRIM_FADE).coerceAtLeast(0f)
                        Pose(scale, -offset * (1f - e), ty, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA * scrimFade)
                    }
                }
            }

            BackPhase.Cancel -> {
                val scale = gestureScale + (1f - gestureScale) * f
                val ty = gestureTy * (1f - f)
                when (role) {
                    BackRole.Top -> Pose(scale, gestureTx * (1f - f), ty, cornerRadius = cornerRadius / scale)
                    else -> Pose(scale, -offset, ty, scrimAlpha = OpalMotion.BACK_SCRIM_ALPHA)
                }
            }
        }
    }
}

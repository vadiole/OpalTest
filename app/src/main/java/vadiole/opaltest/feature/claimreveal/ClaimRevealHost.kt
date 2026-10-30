package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import vadiole.opaltest.ui.common.rememberOpalBitmap
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ClaimRevealHost(state: ClaimRevealState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(state, context) {
        val feedback = RevealFeedback(context)
        state.feedback = feedback
        onDispose {
            state.feedback = null
            feedback.release()
        }
    }
    state.prefetchedGem?.let { rememberRevealImage(it) }
    val focusManager = LocalFocusManager.current
    val contentFocus = remember { FocusRequester() }
    val continueFocus = remember { FocusRequester() }
    val focus = remember { RevealFocus() }
    val blocking = state.activeKey != null && !state.landed
    LaunchedEffect(blocking) {
        when {
            blocking -> {
                focus.overlay = false
                if (focus.content && !continueFocus.requestFocus()) focusManager.clearFocus(force = true)
            }

            focus.overlay -> contentFocus.requestFocus()
        }
    }
    Box(modifier.onPlaced { state.host = it }) {
        Box(
            propagateMinConstraints = true,
            modifier = Modifier
                .graphicsLayer {
                    val radius = BACKDROP_BLUR.toPx() * state.scrim.value
                    renderEffect = if (radius >= 1f) BlurEffect(radius, radius, TileMode.Clamp) else null
                }
                .onFocusChanged { focus.content = it.hasFocus }
                .focusRequester(contentFocus)
                .focusProperties { if (blocking) onEnter = { cancelFocusChange() } }
                .focusGroup()
                .then(if (blocking) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            content()
        }
        val key = state.activeKey
        val revealContent = state.content
        if (key != null && revealContent != null) {
            RevealOverlay(
                state = state,
                key = key,
                content = revealContent,
                continueFocus = continueFocus,
                modifier = Modifier.onFocusChanged { focus.overlay = it.hasFocus }
            )
        }
    }
}

private class RevealFocus {
    var content = false
    var overlay = false
}

fun Modifier.claimRevealSource(state: ClaimRevealState, key: Any): Modifier = this
    .then(ClaimRevealSourceElement(state, key))
    .graphicsLayer { alpha = if (state.isFlying(key)) 1f - state.fade.value else 1f }

private data class ClaimRevealSourceElement(val state: ClaimRevealState, val key: Any) :
    ModifierNodeElement<ClaimRevealSourceNode>() {
    override fun create() = ClaimRevealSourceNode(state, key)

    override fun update(node: ClaimRevealSourceNode) = node.update(state, key)

    override fun InspectorInfo.inspectableProperties() {
        name = "claimRevealSource"
        properties["key"] = key
    }
}

private class ClaimRevealSourceNode(private var state: ClaimRevealState, private var key: Any) :
    Modifier.Node(),
    LayoutAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null

    override fun onPlaced(coordinates: LayoutCoordinates) {
        if (coordinates === this.coordinates) return
        unregister()
        this.coordinates = coordinates
        state.registerSource(key, coordinates)
    }

    override fun onDetach() {
        unregister()
        coordinates = null
    }

    fun update(state: ClaimRevealState, key: Any) {
        if (state === this.state && key == this.key) return
        unregister()
        this.state = state
        this.key = key
        coordinates?.let { state.registerSource(key, it) }
    }

    private fun unregister() {
        coordinates?.let { state.unregisterSource(key, it) }
    }
}

@Composable
private fun rememberRevealImage(@DrawableRes id: Int): ImageBitmap? =
    rememberOpalBitmap(id, software = true, mipmap = true)

@Composable
private fun RevealOverlay(
    state: ClaimRevealState,
    key: Any,
    content: RevealContent,
    continueFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    val gem = rememberRevealImage(content.gem)
    SideEffect { state.gemReady = gem != null }
    LaunchedEffect(continueFocus) {
        snapshotFlow { state.continueClickable }.first { it }
        continueFocus.requestFocus()
    }
    val scrimColor = OpalTheme.colors.background
    val scrimTint = OpalTheme.colors.fillSubtle
    val colors = OpalTheme.colors
    PredictiveBackHandler(enabled = !state.landed) { events ->
        if (state.phase == RevealPhase.Closing) {
            events.collect {}
            return@PredictiveBackHandler
        }
        try {
            events.collect { state.onBackProgress(it.progress) }
            state.onBackCommitted()
        } catch (e: CancellationException) {
            state.onBackCancelled()
            throw e
        }
    }
    Box(modifier.fillMaxSize()) {
        Spacer(
            Modifier
                .fillMaxSize()
                .then(if (state.landed) Modifier else Modifier.blockTouches())
                .drawBehind {
                    drawRect(scrimColor, alpha = SCRIM_ALPHA * state.scrim.value)
                    drawRect(scrimTint, alpha = state.scrim.value)
                }
        )
        if (gem != null) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .gemAmbient(state, key, gem)
            )
            Spacer(
                Modifier
                    .fillMaxSize()
                    .gemGlow(state, key, gem)
            )
            GemSparkles(state = state, frame = { state.gemFrame(key) })
        }
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(GrainBrush, alpha = GRAIN_ALPHA * state.scrim.value) }
        )
        RevealLayout(state = state, title = stringResource(content.title), continueFocus = continueFocus)
        if (gem != null) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .gemImage(state, key, gem, colors)
            )
        }
    }
}

private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() }
}

private const val SCRIM_ALPHA = 0.9f
private val GrainBrush: ShaderBrush by lazy {
    val random = Random(GRAIN_SEED)
    val pixels = IntArray(GRAIN_SIZE * GRAIN_SIZE) { if (random.nextBoolean()) GRAIN_LIGHT else GRAIN_DARK }
    val bitmap = Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}
private const val GRAIN_SIZE = 128
private const val GRAIN_SEED = 7
private const val GRAIN_ALPHA = 0.04f
private const val GRAIN_LIGHT = 0xFFFFFFFF.toInt()
private const val GRAIN_DARK = 0xFF000000.toInt()
private val BACKDROP_BLUR = 14.dp

package vadiole.opaltest.feature.claimreveal

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.lerp
import androidx.window.core.layout.WindowSizeClass
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.exp
import kotlin.math.ln
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ClaimRevealHost(state: ClaimRevealState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.onPlaced { state.host = it }) {
        content()
        val key = state.activeKey
        val revealContent = state.content
        if (key != null && revealContent != null) {
            RevealOverlay(state = state, key = key, content = revealContent)
        }
    }
}

fun Modifier.claimRevealSource(state: ClaimRevealState, key: Any): Modifier = this
    .onGloballyPositioned { state.registerSource(key, it) }
    .graphicsLayer { alpha = if (state.isFlying(key)) 0f else 1f }

@Composable
private fun RevealOverlay(state: ClaimRevealState, key: Any, content: RevealContent) {
    val gem = rememberGemBitmap(content.gem)
    val scrimColor = OpalTheme.colors.background
    PredictiveBackHandler(enabled = state.phase != RevealPhase.Closing) { events ->
        try {
            events.collect { state.onBackProgress(it.progress) }
            state.onBackCommitted()
        } catch (e: CancellationException) {
            state.onBackCancelled()
            throw e
        }
    }
    Box(Modifier.fillMaxSize()) {
        Spacer(
            Modifier
                .fillMaxSize()
                .blockTouches()
                .drawBehind { drawRect(scrimColor, alpha = SCRIM_ALPHA * state.scrim.value) }
        )
        RevealLayout(state = state, title = content.title)
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind { drawFlyingGem(state, key, gem) }
        )
    }
}

@Composable
private fun RevealLayout(state: ClaimRevealState, title: String) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val paneTitle = stringResource(R.string.claim_reveal_pane)
    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .semantics { this.paneTitle = paneTitle }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val minHeight = maxHeight
            val wideGem = min(MAX_GEM_SIZE, min(maxHeight * 0.78f, maxWidth * 0.4f))
            val compactGem = min(MAX_GEM_SIZE, min(maxWidth * 0.8f, maxHeight * 0.44f))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (wide) {
                    WideReveal(
                        state = state,
                        title = title,
                        gemSize = wideGem,
                        modifier = Modifier.heightIn(min = minHeight)
                    )
                } else {
                    CompactReveal(
                        state = state,
                        title = title,
                        gemSize = compactGem,
                        modifier = Modifier.heightIn(min = minHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactReveal(state: ClaimRevealState, title: String, gemSize: Dp, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.medium)
    ) {
        Spacer(Modifier.height(OpalTheme.spacing.large))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GemSlot(state, gemSize)
            Spacer(Modifier.height(OpalTheme.spacing.large))
            RevealTexts(state = state, title = title, textAlign = TextAlign.Center)
        }
        ContinueButton(
            state = state,
            modifier = Modifier
                .padding(top = OpalTheme.spacing.xLarge)
                .widthIn(max = MAX_TEXT_WIDTH)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun WideReveal(state: ClaimRevealState, title: String, gemSize: Dp, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge * 2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OpalTheme.spacing.large, vertical = OpalTheme.spacing.medium)
    ) {
        GemSlot(state, gemSize)
        Column(Modifier.widthIn(max = MAX_TEXT_WIDTH)) {
            RevealTexts(state = state, title = title, textAlign = TextAlign.Start)
            ContinueButton(
                state = state,
                modifier = Modifier
                    .padding(top = OpalTheme.spacing.xLarge)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GemSlot(state: ClaimRevealState, size: Dp) {
    Spacer(
        Modifier
            .size(size)
            .onGloballyPositioned { coordinates ->
                val host = state.host?.takeIf { it.isAttached } ?: return@onGloballyPositioned
                state.target = host.localBoundingBoxOf(coordinates, clipBounds = false)
            }
    )
}

@Composable
private fun RevealTexts(state: ClaimRevealState, title: String, textAlign: TextAlign) {
    Column(
        horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.claim_reveal_title, title),
            style = OpalTheme.typography.largeTitle.copy(lineBreak = LineBreak.Heading),
            textAlign = textAlign,
            modifier = Modifier
                .widthIn(max = MAX_TEXT_WIDTH)
                .revealStagger(state, 0)
        )
        Spacer(Modifier.height(OpalTheme.spacing.small))
        Text(
            text = stringResource(R.string.claim_reveal_message),
            style = OpalTheme.typography.callout,
            color = OpalTheme.colors.contentSecondary,
            textAlign = textAlign,
            modifier = Modifier
                .widthIn(max = MAX_TEXT_WIDTH)
                .revealStagger(state, 1)
        )
    }
}

@Composable
private fun ContinueButton(state: ClaimRevealState, modifier: Modifier = Modifier) {
    OpalButton(
        text = stringResource(R.string.claim_reveal_continue),
        onClick = { if (state.items.last().value >= CLICKABLE_VISIBILITY) state.close() },
        modifier = modifier
            .revealStagger(state, 2)
            .pressScale()
    )
}

private fun Modifier.revealStagger(state: ClaimRevealState, index: Int): Modifier = graphicsLayer {
    val visible = state.items[index].value * (1f - state.back.value)
    alpha = visible
    translationY = (1f - visible) * TEXT_RISE.toPx()
}

private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() }
}

@Composable
private fun rememberGemBitmap(id: Int): ImageBitmap {
    val bitmap = ImageBitmap.imageResource(id)
    return remember(bitmap) { bitmap.also { it.asAndroidBitmap().setHasMipMap(true) } }
}

private fun DrawScope.drawFlyingGem(state: ClaimRevealState, key: Any, gem: ImageBitmap) {
    val target = state.target ?: return
    val source = state.sourceRect(key)?.gemCrop() ?: target
    val progress = state.displayedFlight()
    val size = exp(lerp(ln(source.width.coerceAtLeast(1f)), ln(target.width.coerceAtLeast(1f)), progress))
    val center = arc(source.center, target.center, progress)
    translate(center.x - size / 2f, center.y - size / 2f) {
        scale(size / gem.width, pivot = Offset.Zero) {
            drawImage(gem)
        }
    }
}

private fun arc(start: Offset, end: Offset, t: Float): Offset {
    val control = lerp((start + end) / 2f, Offset(start.x, end.y), ARC_LIFT)
    val u = 1f - t
    return start * (u * u) + control * (2f * u * t) + end * (t * t)
}

private fun Rect.gemCrop(): Rect = Rect(
    left = left + width * GEM_CROP_LEFT,
    top = top + height * GEM_CROP_TOP,
    right = left + width * (GEM_CROP_LEFT + GEM_CROP_SIZE),
    bottom = top + height * (GEM_CROP_TOP + GEM_CROP_SIZE)
)

private const val GEM_CROP_LEFT = 684f / 4096f
private const val GEM_CROP_TOP = 668f / 4096f
private const val GEM_CROP_SIZE = 2600f / 4096f
private const val SCRIM_ALPHA = 0.9f
private const val ARC_LIFT = 0.45f
private const val CLICKABLE_VISIBILITY = 0.5f
private val TEXT_RISE = 12.dp
private val MAX_GEM_SIZE = 320.dp
private val MAX_TEXT_WIDTH = 400.dp

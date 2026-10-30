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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
    val context = LocalContext.current
    DisposableEffect(state, context) {
        val feedback = RevealFeedback(context)
        state.feedback = feedback
        onDispose {
            state.feedback = null
            feedback.release()
        }
    }
    Box(modifier.onPlaced { state.host = it }) {
        Box(
            propagateMinConstraints = true,
            modifier = Modifier.graphicsLayer {
                val radius = BACKDROP_BLUR.toPx() * state.scrim.value
                renderEffect = if (radius >= 1f) BlurEffect(radius, radius, TileMode.Clamp) else null
            }
        ) {
            content()
        }
        val key = state.activeKey
        val revealContent = state.content
        if (key != null && revealContent != null) {
            RevealOverlay(state = state, key = key, content = revealContent)
        }
    }
}

fun Modifier.claimRevealSource(state: ClaimRevealState, key: Any): Modifier = this
    .onGloballyPositioned { state.registerSource(key, it) }
    .graphicsLayer { alpha = if (state.isFlying(key)) 1f - state.fade.value else 1f }

@Composable
private fun RevealOverlay(state: ClaimRevealState, key: Any, content: RevealContent) {
    val gem = rememberGemBitmap(content.gem)
    val scrimColor = OpalTheme.colors.background
    val scrimTint = OpalTheme.colors.fillSubtle
    val lightColor = OpalTheme.colors.glow
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
                .then(if (state.landed) Modifier else Modifier.blockTouches())
                .drawBehind {
                    drawRect(scrimColor, alpha = SCRIM_ALPHA * state.scrim.value)
                    drawRect(scrimTint, alpha = state.scrim.value)
                }
        )
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = state.glow.value * AMBIENT_ALPHA
                    val radius = AMBIENT_BLUR.toPx()
                    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                }
                .drawBehind {
                    val frame = state.gemFrame(key) ?: return@drawBehind
                    drawAmbient(gem, frame, state.glow.value)
                }
        )
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val frame = state.gemFrame(key)
                    alpha = state.glow.value * GLOW_ALPHA
                    val radius = (frame?.width ?: 0f) * GLOW_BLUR
                    renderEffect = if (radius >= 1f) BlurEffect(radius, radius, TileMode.Decal) else null
                }
                .drawBehind {
                    val frame = state.gemFrame(key) ?: return@drawBehind
                    drawGlow(gem, frame, lerp(GLOW_START_SCALE, GLOW_END_SCALE, state.glow.value))
                }
        )
        RevealLayout(state = state, title = content.title)
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (state.landed) return@drawBehind
                    val frame = state.gemFrame(key) ?: return@drawBehind
                    drawGem(state, gem, frame, lightColor)
                }
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
    translationY = if (state.reducedMotion) 0f else (1f - visible) * TEXT_RISE.toPx()
}

private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() }
}

@Composable
private fun rememberGemBitmap(id: Int): ImageBitmap {
    val bitmap = ImageBitmap.imageResource(id)
    return remember(bitmap) { bitmap.also { it.asAndroidBitmap().setHasMipMap(true) } }
}

private fun ClaimRevealState.gemFrame(key: Any): Rect? {
    val target = target ?: return null
    val source = sourceRect(key)?.gemCrop() ?: target
    val progress = displayedFlight()
    val size = exp(lerp(ln(source.width.coerceAtLeast(1f)), ln(target.width.coerceAtLeast(1f)), progress))
    return Rect(center = arc(source.center, target.center, progress), radius = size / 2f)
}

private fun DrawScope.drawGem(state: ClaimRevealState, gem: ImageBitmap, frame: Rect, lightColor: Color) {
    val light = state.charge.value
    val sweep = state.sweep.value
    val sweeping = sweep > 0f && sweep < 1f
    translate(frame.left, frame.top) {
        scale(frame.width / gem.width, pivot = Offset.Zero) {
            if (light < MIN_LIGHT && !sweeping) {
                drawImage(gem, alpha = state.fade.value)
                return@scale
            }
            val bounds = Rect(Offset.Zero, Size(gem.width.toFloat(), gem.height.toFloat()))
            drawIntoCanvas { it.saveLayer(bounds, Paint().apply { alpha = state.fade.value }) }
            drawImage(gem)
            if (light >= MIN_LIGHT) {
                drawRect(
                    brush = Brush.radialGradient(
                        0f to Color.White.copy(alpha = LIGHT_CORE_ALPHA * light),
                        0.45f to lightColor.copy(alpha = LIGHT_EDGE_ALPHA * light),
                        1f to Color.Transparent,
                        center = Offset(bounds.width * GEM_CENTER_X, bounds.height * GEM_CENTER_Y),
                        radius = bounds.width * LIGHT_RADIUS
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
            if (sweeping) {
                drawRect(sweepBrush(bounds.size, sweep), blendMode = BlendMode.SrcAtop)
            }
            drawIntoCanvas { it.restore() }
        }
    }
}

private fun sweepBrush(size: Size, progress: Float): Brush {
    val direction = SweepDirection * size.width
    val center = lerp(
        Offset(size.width / 2f, size.height * GEM_CENTER_Y) - direction * SWEEP_TRAVEL,
        Offset(size.width / 2f, size.height * GEM_CENTER_Y) + direction * SWEEP_TRAVEL,
        progress
    )
    val half = direction * SWEEP_HALF_WIDTH
    return Brush.linearGradient(
        0f to Color.Transparent,
        0.28f to Color.White.copy(alpha = SWEEP_SHEEN_ALPHA),
        0.44f to SweepWarm.copy(alpha = SWEEP_FRINGE_ALPHA),
        0.5f to Color.White.copy(alpha = SWEEP_CORE_ALPHA),
        0.56f to SweepCool.copy(alpha = SWEEP_FRINGE_ALPHA),
        0.72f to Color.White.copy(alpha = SWEEP_SHEEN_ALPHA),
        1f to Color.Transparent,
        start = center - half,
        end = center + half
    )
}

private fun DrawScope.drawGlow(gem: ImageBitmap, frame: Rect, scale: Float) {
    val size = frame.width * scale
    translate(frame.center.x - size / 2f, frame.center.y - size / 2f) {
        scale(size / gem.width, pivot = Offset.Zero) {
            drawImage(gem, colorFilter = GlowSaturation)
        }
    }
}

private fun DrawScope.drawAmbient(gem: ImageBitmap, frame: Rect, glow: Float) {
    val stone = Rect(
        left = frame.left + frame.width * STONE_LEFT,
        top = frame.top + frame.height * STONE_TOP,
        right = frame.left + frame.width * STONE_RIGHT,
        bottom = frame.top + frame.height * STONE_BOTTOM
    )
    val screen = size.toRect().inflate(size.minDimension * AMBIENT_BLEED)
    val area = lerp(stone, screen, glow)
    drawImage(
        image = gem,
        srcOffset = IntOffset((gem.width * STONE_LEFT).toInt(), (gem.height * STONE_TOP).toInt()),
        srcSize = IntSize(
            (gem.width * (STONE_RIGHT - STONE_LEFT)).toInt(),
            (gem.height * (STONE_BOTTOM - STONE_TOP)).toInt()
        ),
        dstOffset = IntOffset(area.left.toInt(), area.top.toInt()),
        dstSize = IntSize(area.width.toInt(), area.height.toInt()),
        colorFilter = AmbientSaturation
    )
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
private const val GLOW_ALPHA = 0.85f
private const val GLOW_BLUR = 0.16f
private const val GLOW_START_SCALE = 0.7f
private const val GLOW_END_SCALE = 1.35f
private const val AMBIENT_ALPHA = 0.14f
private const val AMBIENT_BLEED = 0.1f
private const val STONE_LEFT = 0.2f
private const val STONE_TOP = 0.06f
private const val STONE_RIGHT = 0.8f
private const val STONE_BOTTOM = 0.8f
private val AMBIENT_BLUR = 72.dp
private const val GEM_CENTER_X = 0.5f
private const val GEM_CENTER_Y = 0.42f
private const val LIGHT_RADIUS = 0.42f
private const val LIGHT_CORE_ALPHA = 0.85f
private const val LIGHT_EDGE_ALPHA = 0.45f
private const val MIN_LIGHT = 0.004f
private const val SWEEP_TRAVEL = 0.75f
private const val SWEEP_HALF_WIDTH = 0.32f
private const val SWEEP_SHEEN_ALPHA = 0.05f
private const val SWEEP_FRINGE_ALPHA = 0.13f
private const val SWEEP_CORE_ALPHA = 0.32f
private val SweepDirection = Offset(0.86f, 0.5f)
private val SweepWarm = Color(0xFFFFE1F2)
private val SweepCool = Color(0xFFDDF8FF)
internal val GlowSaturation = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })
private val AmbientSaturation = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.5f) })
private val BACKDROP_BLUR = 14.dp
private const val ARC_LIFT = 0.45f
private const val CLICKABLE_VISIBILITY = 0.5f
private val TEXT_RISE = 12.dp
private val MAX_GEM_SIZE = 320.dp
private val MAX_TEXT_WIDTH = 400.dp

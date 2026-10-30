package vadiole.opaltest.feature.claimreveal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import vadiole.opaltest.ui.theme.OpalColors

internal fun Modifier.gemAmbient(state: ClaimRevealState, key: Any, gem: ImageBitmap): Modifier = this
    .graphicsLayer {
        alpha = state.glow.value * AMBIENT_ALPHA
        val radius = AMBIENT_BLUR.toPx()
        renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
    }
    .drawBehind {
        val frame = state.gemFrame(key) ?: return@drawBehind
        drawAmbient(gem, frame, state.glow.value)
    }

internal fun Modifier.gemGlow(state: ClaimRevealState, key: Any, gem: ImageBitmap): Modifier = this
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

internal fun Modifier.gemImage(state: ClaimRevealState, key: Any, gem: ImageBitmap, colors: OpalColors): Modifier =
    drawBehind {
        if (state.landed) return@drawBehind
        val frame = state.gemFrame(key) ?: return@drawBehind
        scale(state.press.value, pivot = frame.gemCenter()) { drawGem(state, gem, frame, colors) }
    }

@Composable
internal fun GemSlot(state: ClaimRevealState, size: Dp) {
    val pressable = state.phase == RevealPhase.Revealed
    val haptics = LocalHapticFeedback.current
    Spacer(
        Modifier
            .size(size)
            .onGloballyPositioned { coordinates ->
                val host = state.host?.takeIf { it.isAttached } ?: return@onGloballyPositioned
                state.target = host.localBoundingBoxOf(coordinates, clipBounds = false)
            }
            .expandTouchBounds(GEM_TOUCH_EXPANSION)
            .pointerInput(state, pressable, haptics) { if (pressable) trackGemPress(state, haptics) }
    )
}

private fun Modifier.expandTouchBounds(expansion: Dp): Modifier = layout { measurable, constraints ->
    val extra = expansion.roundToPx()
    val placeable = measurable.measure(
        Constraints.fixed(constraints.maxWidth + extra * 2, constraints.maxHeight + extra * 2)
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-extra, -extra) }
}

private suspend fun PointerInputScope.trackGemPress(state: ClaimRevealState, haptics: HapticFeedback) =
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val expansion = GEM_TOUCH_EXPANSION.roundToPx() * 2
        val slotWidth = size.width - expansion
        if (down.position.x !in slotWidth * STONE_LEFT..slotWidth * STONE_RIGHT + expansion) return@awaitEachGesture
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        state.pressGem()
        try {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) break
                if (!change.pressed) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    break
                }
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
            }
        } finally {
            state.releaseGem()
        }
    }

internal fun ClaimRevealState.gemFrame(key: Any): Rect? {
    val target = target ?: return null
    val source = sourceRect(key)?.gemCrop() ?: target
    val progress = displayedFlight()
    val size = exp(lerp(ln(source.width.coerceAtLeast(1f)), ln(target.width.coerceAtLeast(1f)), progress))
    return Rect(center = arc(source.center, target.center, progress, pathBend), radius = size / 2f)
}

private fun DrawScope.drawGem(state: ClaimRevealState, gem: ImageBitmap, frame: Rect, colors: OpalColors) {
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
            drawIntoCanvas { it.saveLayer(bounds, GemLayerPaint.apply { alpha = state.fade.value }) }
            drawImage(gem)
            if (light >= MIN_LIGHT) {
                drawRect(
                    brush = Brush.radialGradient(
                        0f to colors.content.copy(alpha = LIGHT_CORE_ALPHA * light),
                        0.45f to colors.glow.copy(alpha = LIGHT_EDGE_ALPHA * light),
                        1f to Color.Transparent,
                        center = Offset(bounds.width * GEM_CENTER_X, bounds.height * GEM_CENTER_Y),
                        radius = bounds.width * LIGHT_RADIUS
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
            if (sweeping) {
                drawRect(sweepBrush(bounds.size, sweep, colors), blendMode = BlendMode.SrcAtop)
            }
            drawIntoCanvas { it.restore() }
        }
    }
}

private fun sweepBrush(size: Size, progress: Float, colors: OpalColors): Brush {
    val direction = SweepDirection * size.width
    val center = lerp(
        Offset(size.width / 2f, size.height * GEM_CENTER_Y) - direction * SWEEP_TRAVEL,
        Offset(size.width / 2f, size.height * GEM_CENTER_Y) + direction * SWEEP_TRAVEL,
        progress
    )
    val half = direction * SWEEP_HALF_WIDTH
    return Brush.linearGradient(
        0f to Color.Transparent,
        0.28f to colors.content.copy(alpha = SWEEP_SHEEN_ALPHA),
        0.44f to colors.sheenWarm.copy(alpha = SWEEP_FRINGE_ALPHA),
        0.5f to colors.content.copy(alpha = SWEEP_CORE_ALPHA),
        0.56f to colors.sheenCool.copy(alpha = SWEEP_FRINGE_ALPHA),
        0.72f to colors.content.copy(alpha = SWEEP_SHEEN_ALPHA),
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

private fun arc(start: Offset, end: Offset, t: Float, bend: Float): Offset {
    val corner = if (bend >= 0f) Offset(end.x, start.y) else Offset(start.x, end.y)
    val control = lerp((start + end) / 2f, corner, abs(bend))
    val u = 1f - t
    return start * (u * u) + control * (2f * u * t) + end * (t * t)
}

internal fun Rect.gemCenter(): Offset = Offset(left + width * GEM_CENTER_X, top + height * GEM_CENTER_Y)

private fun Rect.gemCrop(): Rect = Rect(
    left = left + width * GEM_CROP_LEFT,
    top = top + height * GEM_CROP_TOP,
    right = left + width * (GEM_CROP_LEFT + GEM_CROP_SIZE),
    bottom = top + height * (GEM_CROP_TOP + GEM_CROP_SIZE)
)

private const val GEM_CROP_LEFT = 684f / 4096f
private const val GEM_CROP_TOP = 668f / 4096f
private const val GEM_CROP_SIZE = 2600f / 4096f
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
private val GemLayerPaint = Paint()
internal val GlowSaturation = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })
private val AmbientSaturation = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.5f) })
private val GEM_TOUCH_EXPANSION = 16.dp

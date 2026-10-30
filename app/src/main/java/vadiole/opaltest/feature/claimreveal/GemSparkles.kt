package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun GemSparkles(state: ClaimRevealState, frame: () -> Rect?, modifier: Modifier = Modifier) {
    val colors = OpalTheme.colors
    val density = LocalDensity.current.density
    val sprites = remember(density, colors.content, colors.glow) {
        SparkleSprites(ceil(SPARKLE_MAX_SIZE_DP * density * SPRITE_SCALE).toInt(), colors.content, colors.glow)
    }
    val motes = remember { FloatArray(SPARKLE_COUNT * MOTE_STRIDE) }
    val bounds = remember { MutableRect(0f, 0f, 0f, 0f) }
    LaunchedEffect(state) {
        snapshotFlow { state.sparkle.value > 0f }.collectLatest { active ->
            if (!active) return@collectLatest
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    state.sparkleTime += (now - last) / NANOS_PER_SECOND / state.timeScale
                    last = now
                }
            }
        }
    }
    Spacer(
        modifier
            .fillMaxSize()
            .drawBehind {
                val visible = state.sparkle.value * (1f - state.back.value)
                if (visible <= 0f) return@drawBehind
                val gem = frame() ?: return@drawBehind
                if (!placeSparkles(state.sparkleTime, gem.width, density, motes, bounds)) return@drawBehind
                val center = gem.gemCenter()
                drawIntoCanvas { sprites.draw(it.nativeCanvas, center, motes, bounds, visible * SPARKLE_ALPHA) }
            }
    )
}

private class SparkleSprites(private val radius: Int, core: Color, tint: Color) {
    private val span = radius * 2
    private val cell = span + SPRITE_PADDING * 2
    private val bitmap = Bitmap.createBitmap(paintSprites(core, tint), cell, cell * (SPRITE_LEVELS + 1), ARGB_8888)
    private val layer = Paint()
    private val sprite = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS }
    private val src = android.graphics.Rect()
    private val dst = RectF()

    fun draw(canvas: Canvas, center: Offset, motes: FloatArray, bounds: MutableRect, alpha: Float) {
        layer.alpha = (alpha * 255f).roundToInt()
        canvas.saveLayer(
            center.x + bounds.left,
            center.y + bounds.top,
            center.x + bounds.right,
            center.y + bounds.bottom,
            layer
        )
        for (i in 0 until SPARKLE_COUNT) {
            val at = i * MOTE_STRIDE
            val size = motes[at + 2]
            val weight = motes[at + 3]
            if (size <= 0f || weight <= 0f) continue
            val level = spriteLevel(weight)
            val top = level * cell + SPRITE_PADDING
            src.set(SPRITE_PADDING, top, SPRITE_PADDING + span, top + span)
            val x = center.x + motes[at]
            val y = center.y + motes[at + 1]
            dst.set(x - size, y - size, x + size, y + size)
            sprite.alpha = (weight / spriteWeight(level) * 255f).roundToInt()
            canvas.drawBitmap(bitmap, src, dst, sprite)
        }
        canvas.restore()
    }

    private fun paintSprites(core: Color, tint: Color): IntArray {
        val pixels = IntArray(cell * cell * (SPRITE_LEVELS + 1))
        for (y in 0 until span) {
            for (x in 0 until span) {
                val dx = x + 0.5f - radius
                val dy = y + 0.5f - radius
                val d = (dx * dx + dy * dy) / (radius * radius)
                if (d >= 1f) continue
                val soft = (1f - d) * (1f - d) * (1f - d)
                val hot = exp(-d * SPARKLE_CORE_SHARPNESS) * SPARKLE_CORE_STRENGTH
                val white = hot / (soft + hot)
                val rgb = Color(
                    red = lerp(tint.red, core.red, white),
                    green = lerp(tint.green, core.green, white),
                    blue = lerp(tint.blue, core.blue, white)
                ).toArgb() and 0xFFFFFF
                for (level in 0..SPRITE_LEVELS) {
                    val alpha = (min(spriteWeight(level) * (soft + hot), 1f) * 255f).roundToInt()
                    pixels[(level * cell + SPRITE_PADDING + y) * cell + SPRITE_PADDING + x] = alpha shl 24 or rgb
                }
            }
        }
        return pixels
    }
}

private fun spriteLevel(weight: Float): Int =
    ceil((weight - SPRITE_LINEAR_WEIGHT) / SPRITE_LEVEL_STEP).toInt().coerceIn(0, SPRITE_LEVELS)

private fun spriteWeight(level: Int): Float = SPRITE_LINEAR_WEIGHT + level * SPRITE_LEVEL_STEP

internal fun placeSparkles(time: Float, unit: Float, density: Float, motes: FloatArray, bounds: MutableRect): Boolean {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    for (i in 0 until SPARKLE_COUNT) {
        val seed = i + 1f
        val at = i * MOTE_STRIDE
        motes[at + 2] = 0f
        val life = SPARKLE_LIFE_S * lerp(1f - SPARKLE_LIFE_VARIANCE, 1f + SPARKLE_LIFE_VARIANCE, hash(seed * 1.7f))
        val age = time - hash(seed * 3.1f) * life
        if (age < 0f) continue
        val cycle = floor(age / life)
        val progress = age / life - cycle
        val angle = 2f * PI.toFloat() * (i + hash(seed * 5.3f + cycle * 11.7f)) / SPARKLE_COUNT
        val dx = sin(angle)
        val dy = cos(angle)
        val reach = if (dy > 0f) SPARKLE_DOWN_RADIUS else SPARKLE_UP_RADIUS
        val grain = hash(seed * 7.9f + cycle * 2.3f)
        val size = density * lerp(SPARKLE_MIN_SIZE_DP, SPARKLE_MAX_SIZE_DP, grain * grain)
        val fade = smoothstep(0f, SPARKLE_FADE_IN, progress) * (1f - smoothstep(SPARKLE_FADE_OUT, 1f, progress))
        val brightness = lerp(SPARKLE_MIN_BRIGHTNESS, 1f, hash(seed * 9.7f + cycle * 4.1f))
        val x = dx * unit * lerp(SPARKLE_START_RADIUS, SPARKLE_SIDE_RADIUS, progress)
        val y = dy * unit * lerp(SPARKLE_START_RADIUS, reach, progress)
        motes[at] = x
        motes[at + 1] = y
        motes[at + 2] = size
        motes[at + 3] = fade * brightness
        left = min(left, x - size)
        top = min(top, y - size)
        right = max(right, x + size)
        bottom = max(bottom, y + size)
    }
    bounds.set(left, top, right, bottom)
    return left < right
}

private fun hash(value: Float): Float {
    var p = fract(value * 0.1031f)
    p *= p + 33.33f
    p *= p + p
    return fract(p)
}

private fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

private fun fract(value: Float): Float = value - floor(value)

private fun smoothstep(from: Float, to: Float, value: Float): Float {
    val t = ((value - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal const val SPARKLE_COUNT = 52
internal const val SPARKLE_DOWN_RADIUS = 0.54f
internal const val SPARKLE_MAX_SIZE_DP = 4.2f
private const val MOTE_STRIDE = 4
private const val SPARKLE_LIFE_S = 8.4f
private const val SPARKLE_LIFE_VARIANCE = 0.25f
private const val SPARKLE_START_RADIUS = 0.18f
private const val SPARKLE_SIDE_RADIUS = 0.66f
private const val SPARKLE_UP_RADIUS = 0.8f
private const val SPARKLE_FADE_IN = 0.15f
private const val SPARKLE_FADE_OUT = 0.55f
private const val SPARKLE_MIN_SIZE_DP = 3f
private const val SPARKLE_MIN_BRIGHTNESS = 0.45f
private const val SPARKLE_CORE_SHARPNESS = 10f
private const val SPARKLE_CORE_STRENGTH = 0.5f
private const val SPARKLE_ALPHA = 0.85f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val SPRITE_PADDING = 1
private const val SPRITE_SCALE = 4f
private const val SPRITE_LEVELS = 8
private const val SPRITE_LINEAR_WEIGHT = 1f / (1f + SPARKLE_CORE_STRENGTH)
private const val SPRITE_LEVEL_STEP = (1f - SPRITE_LINEAR_WEIGHT) / SPRITE_LEVELS

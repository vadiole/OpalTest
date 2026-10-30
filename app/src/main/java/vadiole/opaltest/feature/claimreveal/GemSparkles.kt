package vadiole.opaltest.feature.claimreveal

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun GemSparkles(state: ClaimRevealState, frame: () -> Rect?, modifier: Modifier = Modifier) {
    val colors = OpalTheme.colors
    val shader = remember { RuntimeShader(SparkleShader) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val motes = remember { FloatArray(SPARKLE_COUNT * MOTE_STRIDE) }
    val bounds = remember { MutableRect(0f, 0f, 0f, 0f) }
    val core = colors.content.toArgb()
    val tint = colors.glow.toArgb()
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        snapshotFlow { state.sparkle.value > 0f }.collectLatest { active ->
            if (!active) return@collectLatest
            time.floatValue = 0f
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    time.floatValue += (now - last) / NANOS_PER_SECOND / state.timeScale
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
                if (!placeSparkles(time.floatValue, gem.width, density, motes, bounds)) return@drawBehind
                val center = gem.gemCenter()
                shader.setFloatUniform("center", center.x, center.y)
                shader.setFloatUniform("motes", motes)
                shader.setColorUniform("core", core)
                shader.setColorUniform("tint", tint)
                drawRect(
                    brush = brush,
                    topLeft = center + bounds.topLeft,
                    size = bounds.size,
                    alpha = visible * SPARKLE_ALPHA
                )
            }
    )
}

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

private val SparkleShader = """
    uniform float2 center;
    uniform float4 motes[$SPARKLE_COUNT];
    layout(color) uniform half4 core;
    layout(color) uniform half4 tint;

    half4 main(float2 position) {
        float2 offset = position - center;
        float light = 0.0;
        float heat = 0.0;
        for (int i = 0; i < $SPARKLE_COUNT; i++) {
            float4 mote = motes[i];
            float2 q = offset - mote.xy;
            float d2 = dot(q, q);
            float r2 = mote.z * mote.z;
            if (d2 >= r2) continue;
            float d = d2 / r2;
            float soft = (1.0 - d) * (1.0 - d) * (1.0 - d);
            float hot = exp(-d * $SPARKLE_CORE_SHARPNESS) * $SPARKLE_CORE_STRENGTH;
            light += mote.w * (soft + hot);
            heat += mote.w * hot;
        }
        float alpha = min(light, 1.0);
        float white = light > 0.0 ? min(heat / light, 1.0) : 0.0;
        return half4(mix(tint.rgb, core.rgb, white) * alpha, alpha);
    }
""".trimIndent()

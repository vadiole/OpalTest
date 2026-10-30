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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun GemSparkles(state: ClaimRevealState, frame: () -> Rect?, modifier: Modifier = Modifier) {
    val colors = OpalTheme.colors
    val shader = remember { RuntimeShader(SparkleShader) }
    val brush = remember(shader) { ShaderBrush(shader) }
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
                val center = gem.gemCenter()
                val reach = gem.width * SPARKLE_END_RADIUS + SPARKLE_MAX_SIZE.toPx()
                shader.setFloatUniform("center", center.x, center.y)
                shader.setFloatUniform("unit", gem.width)
                shader.setFloatUniform("density", density)
                shader.setFloatUniform("time", time.floatValue)
                shader.setColorUniform("core", core)
                shader.setColorUniform("tint", tint)
                drawRect(
                    brush = brush,
                    topLeft = center - Offset(reach, reach),
                    size = Size(reach * 2f, reach * 2f),
                    alpha = visible * SPARKLE_ALPHA
                )
            }
    )
}

private const val SPARKLE_COUNT = 22
private const val SPARKLE_LIFE_S = 4.6f
private const val SPARKLE_LIFE_VARIANCE = 0.3f
private const val SPARKLE_START_RADIUS = 0.18f
private const val SPARKLE_END_RADIUS = 0.62f
private const val SPARKLE_FADE_IN = 0.18f
private const val SPARKLE_FADE_OUT = 0.5f
private const val SPARKLE_MIN_SIZE_DP = 4f
private const val SPARKLE_MAX_SIZE_DP = 11f
private val SPARKLE_MAX_SIZE = SPARKLE_MAX_SIZE_DP.dp
private const val SPARKLE_RAY_STRENGTH = 0.55f
private const val SPARKLE_CORE_SHARPNESS = 22f
private const val SPARKLE_TWINKLE = 0.6f
private const val SPARKLE_TWINKLE_RATE = 5f
private const val SPARKLE_ALPHA = 0.9f
private const val NANOS_PER_SECOND = 1_000_000_000f

private val SparkleShader = """
    uniform float2 center;
    uniform float unit;
    uniform float density;
    uniform float time;
    layout(color) uniform half4 core;
    layout(color) uniform half4 tint;

    float hash(float p) {
        p = fract(p * 0.1031);
        p *= p + 33.33;
        p *= p + p;
        return fract(p);
    }

    half4 main(float2 position) {
        float2 offset = position - center;
        float light = 0.0;
        float heat = 0.0;
        for (int i = 0; i < $SPARKLE_COUNT; i++) {
            float seed = float(i) + 1.0;
            float life = $SPARKLE_LIFE_S * (1.0 + $SPARKLE_LIFE_VARIANCE * (2.0 * hash(seed * 1.7) - 1.0));
            float age = time - hash(seed * 3.1) * life;
            if (age < 0.0) continue;
            float cycle = floor(age / life);
            float progress = age / life - cycle;
            float angle = 6.2831853 * hash(seed * 5.3 + cycle * 11.7);
            float radius = unit * mix($SPARKLE_START_RADIUS, $SPARKLE_END_RADIUS, progress);
            float grain = hash(seed * 7.9 + cycle * 2.3);
            float size = density * mix($SPARKLE_MIN_SIZE_DP, $SPARKLE_MAX_SIZE_DP, grain);
            float2 q = (offset - float2(cos(angle), sin(angle)) * radius) / size;
            float d2 = dot(q, q);
            if (d2 >= 1.0) continue;
            float fade = smoothstep(0.0, $SPARKLE_FADE_IN, progress) * (1.0 - smoothstep($SPARKLE_FADE_OUT, 1.0, progress));
            float rate = $SPARKLE_TWINKLE_RATE * (0.6 + 0.8 * hash(seed * 9.7));
            float twinkle = 1.0 - $SPARKLE_TWINKLE * (0.5 + 0.5 * sin(time * rate + 6.2831853 * hash(seed * 13.1)));
            float2 a = abs(q);
            float rays = exp(-a.x * 18.0) * (1.0 - a.y) * (1.0 - a.y) + exp(-a.y * 18.0) * (1.0 - a.x) * (1.0 - a.x);
            float halo = (1.0 - d2) * (1.0 - d2) * 0.25;
            float hot = exp(-d2 * $SPARKLE_CORE_SHARPNESS);
            float ray = rays * $SPARKLE_RAY_STRENGTH * smoothstep(0.35, 0.8, grain);
            float strength = fade * twinkle;
            light += strength * (hot + ray + halo);
            heat += strength * hot;
        }
        float alpha = min(light, 1.0);
        float white = light > 0.0 ? min(heat / light, 1.0) : 0.0;
        return half4(mix(tint.rgb, core.rgb, white) * alpha, alpha);
    }
""".trimIndent()

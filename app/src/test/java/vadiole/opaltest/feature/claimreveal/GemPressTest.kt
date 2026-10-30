package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class GemPressTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: ClaimRevealState

    private val haptics = RecordingHaptics()

    private fun setContent() {
        compose.setContent {
            state = rememberClaimRevealState()
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                OpalTheme {
                    ClaimRevealHost(state = state, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            state.open(
                KEY,
                RevealContent(gem = R.drawable.img_milestone_loyal_large, title = R.string.referral_milestone_loyal)
            )
        }
    }

    private fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun advance(millis: Long) = repeat((millis / 16).toInt()) { frame() }

    private fun awaitTarget() {
        val deadline = System.currentTimeMillis() + 10_000
        while (state.target == null || !state.gemReady) {
            check(System.currentTimeMillis() < deadline) { "gem never decoded" }
            Thread.sleep(10)
            frame()
        }
    }

    private fun awaitRevealed() {
        awaitTarget()
        repeat(600) {
            frame()
            if (state.phase == RevealPhase.Revealed) return frame()
        }
        error("reveal never settled")
    }

    private fun gemCenter(): Offset = state.target!!.center

    private fun scale() = state.press.value

    private fun down(position: Offset) = compose.onRoot().performTouchInput { down(position) }

    private fun up() = compose.onRoot().performTouchInput { up() }

    private fun dp(value: Float) = value * compose.density.density

    @Test
    fun `press shrinks the gem and release bounces back past rest`() {
        setContent()
        awaitRevealed()
        down(gemCenter())
        advance(300)
        assertEquals(PRESSED, scale(), 0.002f)
        up()
        var peak = 0f
        repeat(50) {
            frame()
            peak = maxOf(peak, scale())
        }
        assertTrue("release should overshoot, peak $peak", peak > 1.004f)
        assertEquals(1f, scale(), 0.001f)
    }

    @Test
    fun `pressing mid-bounce continues from the current value`() {
        setContent()
        awaitRevealed()
        down(gemCenter())
        advance(300)
        up()
        advance(80)
        val before = scale()
        assertTrue(abs(before - 1f) > 0.005f && abs(before - PRESSED) > 0.005f)
        down(gemCenter())
        frame()
        assertTrue("jumped from $before to ${scale()}", abs(scale() - before) < 0.01f)
        advance(400)
        assertEquals(PRESSED, scale(), 0.002f)
        up()
        advance(1000)
        assertEquals(1f, scale(), 0.001f)
    }

    @Test
    fun `dragging past touch slop releases the gem`() {
        setContent()
        awaitRevealed()
        down(gemCenter())
        advance(200)
        compose.onRoot().performTouchInput { moveBy(Offset(0f, -viewConfiguration.touchSlop * 3)) }
        advance(1000)
        assertEquals(1f, scale(), 0.001f)
        up()
    }

    @Test
    fun `press outside the stone or before the gem settles is ignored`() {
        setContent()
        awaitTarget()
        assertTrue(state.phase == RevealPhase.Opening)
        down(gemCenter())
        advance(200)
        assertEquals(1f, scale(), 0f)
        up()
        awaitRevealed()
        val target = state.target!!
        down(Offset(target.left + target.width * 0.05f, target.center.y))
        advance(200)
        assertEquals(1f, scale(), 0f)
        up()
    }

    @Test
    fun `press just outside the gem bounds still squishes`() {
        setContent()
        awaitRevealed()
        val target = state.target!!
        down(Offset(target.center.x, target.top - dp(12f)))
        advance(300)
        assertEquals(PRESSED, scale(), 0.002f)
        up()
        advance(1000)
        down(Offset(target.center.x, target.bottom + dp(12f)))
        advance(300)
        assertEquals(PRESSED, scale(), 0.002f)
        up()
        advance(1000)
        down(Offset(target.center.x, target.top - dp(24f)))
        advance(300)
        assertEquals(1f, scale(), 0f)
        up()
    }

    @Test
    fun `press and release each tick once`() {
        setContent()
        awaitRevealed()
        down(gemCenter())
        frame()
        assertEquals(listOf(HapticFeedbackType.SegmentTick), haptics.performed)
        up()
        frame()
        assertEquals(List(2) { HapticFeedbackType.SegmentTick }, haptics.performed)
    }

    @Test
    fun `dragging away ticks only on press and ignored presses stay silent`() {
        setContent()
        awaitRevealed()
        val target = state.target!!
        down(Offset(target.left + target.width * 0.05f, target.center.y))
        up()
        frame()
        assertEquals(emptyList<HapticFeedbackType>(), haptics.performed)
        down(gemCenter())
        compose.onRoot().performTouchInput { moveBy(Offset(0f, -viewConfiguration.touchSlop * 3)) }
        up()
        frame()
        assertEquals(listOf(HapticFeedbackType.SegmentTick), haptics.performed)
    }

    @Test
    fun `closing while pressed restores the gem`() {
        setContent()
        awaitRevealed()
        down(gemCenter())
        advance(200)
        compose.runOnIdle { state.close() }
        advance(200)
        assertTrue(scale() > PRESSED + 0.02f)
        up()
    }

    @Test
    fun `pressed frame keeps the glow`() {
        setContent()
        awaitRevealed()
        repeat(1000) { if (state.sweep.isRunning || state.sparkle.isRunning) frame() }
        compose.runOnIdle {
            runBlocking {
                state.sparkle.snapTo(0f)
                state.sweep.snapTo(1f)
            }
        }
        frame()
        val rest = capture("claim_reveal_gem_rest")
        down(gemCenter())
        advance(300)
        val pressed = capture("claim_reveal_gem_pressed")
        up()
        val gem = state.target!!
        val around = gem.inflate(gem.width / 2f)
        val columns = around.left.toInt().coerceAtLeast(0) until around.right.toInt().coerceAtMost(rest.width)
        val rows = around.top.toInt().coerceAtLeast(0) until around.bottom.toInt().coerceAtMost(rest.height)
        var outside = 0
        var inside = 0
        for (y in rows) {
            for (x in columns) {
                if (rest.getPixel(x, y) == pressed.getPixel(x, y)) continue
                if (gem.contains(Offset(x + 0.5f, y + 0.5f))) inside++ else outside++
            }
        }
        assertEquals("glow changed around the gem", 0, outside)
        assertTrue("gem did not change", inside > 0)
    }

    private fun capture(name: String): Bitmap {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private companion object {
        const val KEY = "gem"
        const val PRESSED = 0.95f
    }
}

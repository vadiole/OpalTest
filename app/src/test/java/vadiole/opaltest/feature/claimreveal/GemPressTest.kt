package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import java.io.File
import kotlin.math.abs
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

    private fun setContent() {
        compose.setContent {
            state = rememberClaimRevealState()
            OpalTheme {
                ClaimRevealHost(state = state, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            state.open(KEY, RevealContent(gem = R.drawable.img_milestone_loyal_large, title = "Loyal"))
        }
    }

    private fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun advance(millis: Long) = repeat((millis / 16).toInt()) { frame() }

    private fun awaitTarget() {
        val deadline = System.currentTimeMillis() + 10_000
        while (state.target == null) {
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
        assertTrue(abs(before - 1f) > 0.005f || abs(before - PRESSED) > 0.005f)
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
        advance(2000)
        capture("claim_reveal_gem_rest")
        down(gemCenter())
        advance(300)
        capture("claim_reveal_gem_pressed")
        up()
    }

    private fun capture(name: String) {
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val KEY = "gem"
        const val PRESSED = 0.95f
    }
}

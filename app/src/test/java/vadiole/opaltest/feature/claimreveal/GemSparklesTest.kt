package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class GemSparklesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sparkles wait for the gem to settle and then fade in`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitTarget()
        while (reveal.state.phase == RevealPhase.Opening) {
            assertEquals(0f, reveal.state.sparkle.value, 0f)
            reveal.frame()
        }
        reveal.advance(300)
        val rising = reveal.state.sparkle.value
        assertTrue("sparkles should be fading in, $rising", rising > 0f && rising < 1f)
        reveal.advance(1500)
        assertEquals(1f, reveal.state.sparkle.value, 0f)
        reveal.advance(2000)
        reveal.capture("claim_reveal_sparkles")
    }

    @Test
    fun `sparkles fade out before the gem lands`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitRevealed()
        reveal.advance(1500)
        compose.runOnIdle { reveal.state.close() }
        reveal.advance(100)
        val fading = reveal.state.sparkle.value
        assertTrue("sparkles should be fading out, $fading", fading > 0f && fading < 1f)
        reveal.advance(200)
        assertEquals(0f, reveal.state.sparkle.value, 0f)
        assertNotNull(reveal.state.activeKey)
    }

    @Test
    fun `closing mid fade in continues from the current value`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitRevealed()
        reveal.advance(200)
        val before = reveal.state.sparkle.value
        compose.runOnIdle { reveal.state.close() }
        reveal.frame()
        val after = reveal.state.sparkle.value
        assertTrue("jumped from $before to $after", after <= before && before - after < 0.2f)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w874dp-h402dp-xxhdpi")
class GemSparklesLandscapeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sparkles follow the start aligned gem`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitRevealed()
        reveal.advance(3500)
        assertEquals(1f, reveal.state.sparkle.value, 0f)
        reveal.capture("claim_reveal_sparkles_landscape")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class GemSparklesReducedMotionTest {
    @get:Rule
    val compose = createComposeRule(effectContext = NoMotion)

    @Test
    fun `reduced motion shows no sparkles`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitRevealed()
        assertTrue(reveal.state.reducedMotion)
        reveal.advance(2000)
        assertEquals(0f, reveal.state.sparkle.value, 0f)
    }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor = 0f
    }
}

private class SparkleReveal(private val compose: ComposeContentTestRule) {
    lateinit var state: ClaimRevealState

    init {
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
            state.open(
                "gem",
                RevealContent(gem = R.drawable.img_milestone_loyal_large, title = R.string.referral_milestone_loyal)
            )
        }
    }

    fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    fun advance(millis: Long) = repeat((millis / 16).toInt()) { frame() }

    fun awaitTarget() {
        val deadline = System.currentTimeMillis() + 10_000
        while (state.target == null || !state.gemReady) {
            check(System.currentTimeMillis() < deadline) { "gem never decoded" }
            Thread.sleep(10)
            frame()
        }
    }

    fun awaitRevealed() {
        awaitTarget()
        repeat(600) {
            frame()
            if (state.phase == RevealPhase.Revealed) return
        }
        error("reveal never settled")
    }

    fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

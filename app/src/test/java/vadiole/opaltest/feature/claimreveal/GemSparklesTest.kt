package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.runBlocking
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
    }

    @Test
    fun `motes spread around the gem and stay clear of the title`() {
        val reveal = SparkleReveal(compose)
        reveal.awaitRevealed()
        val frames = listOf(2500L, 3000L, 3500L).mapIndexed { index, millis ->
            reveal.advance(millis)
            reveal.capture("claim_reveal_sparkles_$index")
        }
        compose.runOnIdle { runBlocking { reveal.state.sparkle.snapTo(0f) } }
        reveal.frame()
        val plain = reveal.capture("claim_reveal_sparkles_off")
        val slot = reveal.state.target!!
        val title = reveal.titleBounds()
        val stoneBottom = slot.top + slot.height * 0.8f
        var left = 0
        var right = 0
        var under = 0
        var onTitle = 0
        frames.forEach { sparkling ->
            for (y in 0 until plain.height) {
                for (x in 0 until plain.width) {
                    if (sparkling.getPixel(x, y) == plain.getPixel(x, y)) continue
                    val pixel = Offset(x + 0.5f, y + 0.5f)
                    when {
                        title.contains(pixel) || y >= title.top -> onTitle++
                        x < slot.left -> left++
                        x >= slot.right -> right++
                        y > stoneBottom -> under++
                    }
                }
            }
        }
        assertTrue("left $left, right $right, under $under", left > 0 && right > 0 && under > 0)
        assertEquals("motes reached the title", 0, onTitle)
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
        val sparkling = reveal.capture("claim_reveal_sparkles_landscape")
        reveal.advance(3000)
        reveal.capture("claim_reveal_sparkles_landscape_later")
        compose.runOnIdle { runBlocking { reveal.state.sparkle.snapTo(0f) } }
        reveal.frame()
        val plain = reveal.capture("claim_reveal_sparkles_landscape_off")
        val title = reveal.titleBounds()
        var onTitle = 0
        for (y in title.top.toInt() until title.bottom.toInt().coerceAtMost(plain.height)) {
            for (x in title.left.toInt() until title.right.toInt().coerceAtMost(plain.width)) {
                if (sparkling.getPixel(x, y) != plain.getPixel(x, y)) onTitle++
            }
        }
        assertEquals("motes reached the title", 0, onTitle)
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

class SparkleFieldTest {
    private val motes = FloatArray(SPARKLE_COUNT * 4)

    @Test
    fun `motes never travel further down than the downward reach`() {
        val limit = SPARKLE_DOWN_RADIUS * UNIT + SPARKLE_MAX_SIZE_DP
        for (step in 0..2400) {
            placeSparkles(step * 0.05f, UNIT, 1f, motes)
            for (i in 0 until SPARKLE_COUNT) {
                val bottom = motes[i * 4 + 1] + motes[i * 4 + 2]
                assertTrue("mote $i at ${step * 0.05f}s reached $bottom", bottom <= limit + 0.01f)
            }
        }
    }

    @Test
    fun `live motes cover every direction around the gem`() {
        for (time in listOf(10f, 25f, 40f)) {
            val octants = IntArray(8)
            placeSparkles(time, UNIT, 1f, motes)
            for (i in 0 until SPARKLE_COUNT) {
                if (motes[i * 4 + 3] < 0.2f) continue
                val angle = atan2(motes[i * 4 + 1], motes[i * 4]) + PI.toFloat()
                octants[(angle / (PI.toFloat() / 4f)).toInt().coerceAtMost(7)]++
            }
            assertTrue("octants at ${time}s: ${octants.toList()}", octants.all { it >= 2 })
        }
    }

    private companion object {
        const val UNIT = 1000f
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

    fun titleBounds(): Rect = compose.onNodeWithText("Loyal MileStone", substring = true, useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot

    fun capture(name: String): Bitmap {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }
}

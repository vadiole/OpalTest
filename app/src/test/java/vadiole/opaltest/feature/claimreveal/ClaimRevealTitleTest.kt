package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ClaimRevealTitleTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: ClaimRevealState

    private fun reveal(fontScale: Float = 1f) {
        compose.setContent {
            state = rememberClaimRevealState()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
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
                RevealContent(gem = R.drawable.img_milestone_popular_large, title = R.string.referral_milestone_popular)
            )
        }
        val deadline = System.currentTimeMillis() + 10_000
        while (state.target == null || !state.gemReady) {
            check(System.currentTimeMillis() < deadline) { "gem never decoded" }
            Thread.sleep(10)
            frame()
        }
        repeat(600) {
            frame()
            if (state.phase == RevealPhase.Revealed) {
                repeat(120) { frame() }
                return
            }
        }
        error("reveal never settled")
    }

    private fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun titleLayout(title: String = TITLE): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!
            .invoke(results)
        return results.single()
    }

    private fun lines(layout: TextLayoutResult) = (0 until layout.lineCount).map {
        layout.layoutInput.text.text.substring(layout.getLineStart(it), layout.getLineEnd(it, visibleEnd = true))
    }

    @Test
    fun `portrait title keeps the gem name on its own line`() {
        reveal()
        capture("claim_reveal_title_portrait")
        assertEquals(listOf(NAME, SUFFIX), lines(titleLayout()))
    }

    @Test
    @Config(qualifiers = "w874dp-h402dp-xxhdpi")
    fun `landscape title keeps the gem name on its own line`() {
        reveal()
        capture("claim_reveal_title_landscape")
        assertEquals(listOf(NAME, SUFFIX), lines(titleLayout()))
    }

    @Test
    fun `large font title wraps the gem name instead of clipping`() {
        reveal(fontScale = 2f)
        capture("claim_reveal_title_large_font")
        val layout = titleLayout()
        val lines = lines(layout)
        assertTrue("expected the name to wrap, got $lines", lines.size > 2)
        assertEquals(SUFFIX, lines.last())
        assertFalse(layout.hasVisualOverflow)
    }

    @Test
    @Config(qualifiers = "fr-w402dp-h874dp-xxhdpi")
    fun `french title keeps the gem name on its own line`() {
        reveal()
        capture("claim_reveal_title_fr")
        assertEquals(listOf("MileStone Populaire", "est à toi"), lines(titleLayout("MileStone Populaire\nest à toi")))
    }

    @Test
    @Config(qualifiers = "es-w402dp-h874dp-xxhdpi")
    fun `spanish title keeps the gem name on its own line`() {
        reveal()
        capture("claim_reveal_title_es")
        assertEquals(listOf("MileStone Popular", "es tuyo"), lines(titleLayout("MileStone Popular\nes tuyo")))
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val KEY = "gem"
        const val NAME = "Popular MileStone"
        const val SUFFIX = "is yours"
        const val TITLE = "$NAME\n$SUFFIX"
    }
}

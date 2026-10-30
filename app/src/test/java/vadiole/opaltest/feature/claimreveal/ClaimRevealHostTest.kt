package vadiole.opaltest.feature.claimreveal

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.ImageSource
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.OpalImageCache
import vadiole.opaltest.ui.common.imageSource
import vadiole.opaltest.ui.common.rememberOpalBitmap
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class ClaimRevealHostTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: ClaimRevealState
    private lateinit var focusManager: FocusManager
    private lateinit var inputMode: InputModeManager
    private lateinit var listState: LazyListState
    private val underneath = FocusRequester()
    private var underneathClicks = 0
    private var warm: ImageBitmap? = null

    private fun setButtonContent(focusable: Boolean = true) {
        compose.setContent {
            state = rememberClaimRevealState()
            warm = rememberOpalBitmap(GEM, software = true, mipmap = true)
            focusManager = LocalFocusManager.current
            inputMode = LocalInputModeManager.current
            OpalTheme {
                ClaimRevealHost(state = state, modifier = Modifier.fillMaxSize()) {
                    if (focusable) {
                        Column {
                            OpalButton(
                                text = UNDERNEATH,
                                onClick = { underneathClicks++ },
                                modifier = Modifier.focusRequester(underneath)
                            )
                            OpalButton(text = SECOND, onClick = {})
                        }
                    } else {
                        Text(UNDERNEATH)
                    }
                }
            }
        }
        awaitWarm()
    }

    private fun setListContent(sourceType: Any? = null) {
        compose.setContent {
            state = rememberClaimRevealState()
            warm = rememberOpalBitmap(GEM, software = true, mipmap = true)
            listState = rememberLazyListState()
            OpalTheme {
                ClaimRevealHost(state = state, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        item(contentType = sourceType) {
                            OpalImage(
                                id = THUMBNAIL,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .claimRevealSource(state, KEY)
                            )
                        }
                        items(40) { Spacer(Modifier.fillMaxWidth().height(200.dp)) }
                    }
                }
            }
        }
        awaitWarm()
    }

    private fun awaitWarm() {
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val deadline = System.currentTimeMillis() + 10_000
        while (warm == null) {
            check(System.currentTimeMillis() < deadline) { "gem never decoded" }
            Thread.sleep(10)
            frame()
        }
    }

    private fun open(gem: Int = GEM) = compose.runOnIdle {
        state.open(KEY, RevealContent(gem = gem, title = R.string.referral_milestone_loyal))
    }

    private fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun advance(millis: Long) = repeat((millis / 16).toInt()) { frame() }

    private fun awaitGem() {
        val deadline = System.currentTimeMillis() + 10_000
        while (state.target == null || !state.gemReady) {
            check(System.currentTimeMillis() < deadline) { "gem never decoded" }
            Thread.sleep(10)
            frame()
        }
    }

    private fun awaitButton() {
        repeat(1000) {
            frame()
            if (state.items.last().value >= 1f) return
        }
        error("continue never appeared")
    }

    private fun awaitFinished() {
        repeat(600) {
            frame()
            if (state.activeKey == null) return frame()
        }
        error("reveal never finished")
    }

    private fun useKeyboard() = compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }

    private fun paneTitle() = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Reward claimed")

    @Test
    fun `content underneath is hidden from accessibility while the reveal is open`() {
        setButtonContent()
        compose.onNodeWithText(UNDERNEATH).assertExists()
        open()
        frame()
        compose.onNodeWithText(UNDERNEATH).assertDoesNotExist()
        compose.onNodeWithText(SECOND).assertDoesNotExist()
        compose.onNode(paneTitle()).assertExists()
        compose.onNodeWithText("Continue").assertExists()
        awaitGem()
        awaitButton()
        compose.runOnIdle { state.close() }
        awaitFinished()
        compose.onNodeWithText(UNDERNEATH).assertExists()
        compose.onNode(paneTitle()).assertDoesNotExist()
    }

    @Test
    fun `keyboard focus leaves the content and cannot move back while the reveal is open`() {
        setButtonContent()
        useKeyboard()
        compose.runOnIdle { underneath.requestFocus() }
        compose.onNodeWithText(UNDERNEATH).assertIsFocused()
        open()
        frame()
        compose.onNodeWithText("Continue").assertIsFocused()
        repeat(4) {
            compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
            compose.onNodeWithText("Continue").assertIsFocused()
            compose.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }
            compose.onNodeWithText("Continue").assertIsFocused()
        }
        compose.runOnIdle { assertFalse(underneath.requestFocus()) }
        compose.onNodeWithText("Continue").assertIsFocused()
        awaitGem()
        awaitButton()
        compose.runOnIdle { state.close() }
        awaitFinished()
        compose.onNodeWithText(UNDERNEATH).assertIsFocused()
    }

    @Test
    fun `continue takes keyboard focus when it appears`() {
        setButtonContent(focusable = false)
        useKeyboard()
        open()
        awaitGem()
        awaitButton()
        frame()
        compose.onNodeWithText("Continue").assertIsFocused()
    }

    @Test
    fun `a gem that never decodes still blocks the content and finishes the reveal`() {
        setButtonContent()
        val button = compose.onNodeWithText(UNDERNEATH).fetchSemanticsNode().boundsInRoot.center
        open(gem = R.drawable.ic_gift)
        frame()
        compose.onNode(paneTitle()).assertExists()
        compose.onNodeWithText(UNDERNEATH).assertDoesNotExist()
        compose.onRoot().performTouchInput { click(button) }
        advance(200)
        assertEquals(0, underneathClicks)
        awaitButton()
        assertFalse(state.gemReady)
        assertEquals(RevealPhase.Revealed, state.phase)
        compose.runOnIdle { state.close() }
        awaitFinished()
    }

    @Test
    fun `prefetch decodes the reveal image before the reveal opens`() {
        setButtonContent()
        compose.runOnIdle { state.prefetch(PREFETCHED) }
        frame()
        frame()
        Thread.sleep(PREFETCH_BUDGET_MS)
        open(gem = PREFETCHED)
        frame()
        assertTrue(state.gemReady)
    }

    @Test
    fun `clearing the prefetch releases the reveal image`() {
        setButtonContent()
        compose.runOnIdle { state.prefetch(PREFETCHED) }
        awaitCached(PREFETCHED, large = true)
        compose.runOnIdle { state.clearPrefetch() }
        frame()
        assertNull(state.prefetchedGem)
        assertNull(cached(PREFETCHED, large = true))
    }

    @Test
    fun `finished reveal evicts only the large image and decodes it again for the next reveal`() {
        setListContent()
        awaitCached(THUMBNAIL, large = false)
        open()
        awaitGem()
        awaitButton()
        assertNotNull(cached(GEM, large = true))
        compose.runOnIdle { state.close() }
        awaitFinished()
        assertNull(cached(GEM, large = true))
        assertNotNull(cached(THUMBNAIL, large = false))
        open()
        awaitGem()
        assertNotNull(cached(GEM, large = true))
    }

    @Test
    fun `a claim started while the gem lands keeps its prefetch after the reveal finishes`() {
        setListContent()
        open()
        awaitGem()
        awaitButton()
        compose.runOnIdle { state.close() }
        repeat(600) { if (!state.landed) frame() }
        assertTrue(state.landed)
        compose.runOnIdle { state.prefetch(PREFETCHED) }
        awaitFinished()
        assertEquals(PREFETCHED, state.prefetchedGem)
        awaitCached(PREFETCHED, large = true)
        assertNull(cached(GEM, large = true))
    }

    private fun cached(id: Int, large: Boolean): ImageBitmap? {
        val source = RuntimeEnvironment.getApplication().resources.imageSource(id) as ImageSource.Bitmap
        return OpalImageCache.bitmaps[source.key.copy(software = large, mipmap = large)]
    }

    private fun awaitCached(id: Int, large: Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (cached(id, large) == null) {
            check(System.currentTimeMillis() < deadline) { "image never cached" }
            Thread.sleep(10)
            frame()
        }
    }

    @Test
    fun `gem flies back into a visible source`() {
        setListContent()
        open()
        awaitGem()
        awaitButton()
        val source = state.sourceRect(KEY)
        assertNotNull(source)
        compose.runOnIdle { state.close() }
        advance(500)
        capture("claim_reveal_flyback_visible_mid")
        assertEquals(1f, state.fade.value, 0f)
        awaitFinished()
        assertNull(state.activeKey)
    }

    @Test
    fun `gem fades out toward the last known rect when the source scrolled away`() {
        setListContent()
        open()
        awaitGem()
        awaitButton()
        val before = state.sourceRect(KEY)
        assertNotNull(before)
        compose.runOnIdle { listState.requestScrollToItem(30) }
        advance(100)
        assertEquals(before, state.sourceRect(KEY))
        compose.runOnIdle { state.close() }
        advance(500)
        capture("claim_reveal_flyback_detached_mid")
        assertTrue("gem should fade, fade ${state.fade.value}", state.fade.value < 0.9f)
        awaitFinished()
        assertNull(state.activeKey)
    }

    @Test
    fun `gem fades out when the source waits in the lazy reuse pool`() {
        setListContent(sourceType = "source")
        open()
        awaitGem()
        awaitButton()
        compose.runOnIdle { listState.requestScrollToItem(30) }
        advance(100)
        compose.runOnIdle { state.close() }
        advance(500)
        assertTrue("gem should fade, fade ${state.fade.value}", state.fade.value < 0.9f)
        awaitFinished()
    }

    @Test
    fun `continue does not take focus in touch mode`() {
        setButtonContent()
        open()
        awaitGem()
        awaitButton()
        frame()
        compose.onNodeWithText("Continue").assertIsNotFocused()
        compose.runOnIdle { state.close() }
        awaitFinished()
        compose.onNodeWithText(UNDERNEATH).assertIsNotFocused()
    }

    @Test
    fun `the shine sweeps once and continue does not wait for it`() {
        setButtonContent()
        open()
        awaitGem()
        var started = false
        var finished = false
        var buttonBeforeFinish = false
        var replays = 0
        var last = 0f
        repeat(1250) {
            frame()
            val sweep = state.sweep.value
            if (sweep > 0f) started = true
            if (sweep < last) replays++
            if (sweep >= 1f) finished = true
            if (!finished && state.items.last().value >= 1f) buttonBeforeFinish = true
            last = sweep
        }
        assertTrue(started && finished)
        assertEquals(0, replays)
        assertTrue("continue waited for the shine", buttonBeforeFinish)
        compose.runOnIdle { state.close() }
        awaitFinished()
    }

    @Test
    fun `closing mid sweep fades the shine out in place`() {
        setButtonContent()
        open()
        awaitGem()
        var frames = 0
        while (state.sweep.value < 0.5f && frames++ < 1000) frame()
        val before = state.sweep.value
        assertTrue("sweep $before", before > 0f && before < 1f)
        capture("claim_reveal_sweep_mid")
        compose.runOnIdle { state.close() }
        advance(160)
        capture("claim_reveal_sweep_closing")
        val fading = state.sweepFade.value
        assertTrue("shine should be fading, $fading", fading > 0f && fading < 1f)
        assertEquals(before, state.sweep.value, 0.02f)
        advance(320)
        assertEquals(0f, state.sweepFade.value, 0f)
        awaitFinished()
        assertEquals(1f, state.sweepFade.value, 0f)
    }

    private fun capture(name: String): Bitmap {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private companion object {
        const val KEY = "gem"
        val GEM = R.drawable.img_milestone_loyal_large
        val THUMBNAIL = R.drawable.img_milestone_loyal
        val PREFETCHED = R.drawable.img_milestone_soulful_large
        const val PREFETCH_BUDGET_MS = 2_000L
        const val UNDERNEATH = "Underneath"
        const val SECOND = "Second"
    }
}

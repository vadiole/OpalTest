package vadiole.opaltest.feature.console

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class ConsoleScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val debug = ReferralDebugState(
        plan = Plan.Free,
        successfulReferrals = 3,
        maxReferrals = 999,
        claimedTiers = 1,
        nextTierAt = 5,
        latency = SimulatedLatency.Normal,
        failNextLoad = false,
        failNextClaim = false
    )

    @Test
    fun phone() = capture("console_phone", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    fun allUnlocked() = capture(
        "console_all_unlocked",
        ConsoleState(ConsoleLoad.Ready(debug.copy(successfulReferrals = 999, claimedTiers = 7, nextTierAt = null)))
    )

    @Test
    fun confirmReset() = capture("console_confirm_reset", ConsoleState(ConsoleLoad.Ready(debug, true)))

    @Test
    fun loading() = capture("console_loading", ConsoleState())

    @Test
    fun error() = capture("console_error", ConsoleState(ConsoleLoad.Failed))

    @Test
    @Config(qualifiers = "w874dp-h402dp-xxhdpi")
    fun landscape() = capture("console_landscape", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet() = capture("console_tablet", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(fontScale = 2f)
    fun largeFont() = capture("console_font_2", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(qualifiers = "w360dp-h300dp-xxhdpi")
    fun smallWindow() = capture("console_360x300", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(qualifiers = "fr-w360dp-h800dp-xxhdpi", fontScale = 1.5f)
    fun french() = capture("console_fr_font_1_5", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(qualifiers = "es-w360dp-h800dp-xxhdpi", fontScale = 1.5f)
    fun spanish() = capture("console_es_font_1_5", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    @Config(qualifiers = "fr-w874dp-h402dp-xxhdpi")
    fun frenchLandscape() = capture("console_fr_landscape", ConsoleState(ConsoleLoad.Ready(debug)))

    @Test
    fun friendCountRoll() {
        var state by mutableStateOf(ConsoleState(ConsoleLoad.Ready(debug)))
        compose.mainClock.autoAdvance = false
        compose.setContent { Content(state) }
        compose.mainClock.advanceTimeBy(2_000)
        state = ConsoleState(ConsoleLoad.Ready(debug.copy(successfulReferrals = 4), isConfirmingReset = true))
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(64)
        save("console_transition_mid")
        compose.mainClock.advanceTimeBy(2_000)
        save("console_transition_end")
    }

    private fun capture(name: String, state: ConsoleState) {
        compose.mainClock.autoAdvance = false
        compose.setContent { Content(state) }
        compose.mainClock.advanceTimeBy(2_000)
        save(name)
    }

    @Composable
    private fun Content(state: ConsoleState) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            OpalTheme { ConsoleContent(state = state, onIntent = {}) }
        }
    }

    private fun save(name: String) {
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

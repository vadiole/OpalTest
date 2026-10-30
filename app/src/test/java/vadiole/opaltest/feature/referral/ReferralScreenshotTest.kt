package vadiole.opaltest.feature.referral

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class ReferralScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w402dp-h1861dp-xxhdpi")
    fun figma() = capture("referral_figma", ReferralState(program = previewProgram()))

    @Test
    fun proNoReferrals() = capture(
        "referral_pro_no_referrals",
        ReferralState(program = previewProgram(plan = Plan.Pro, referrals = 0))
    )

    @Test
    fun claiming() = capture(
        "referral_claiming",
        ReferralState(program = previewProgram(), claimingTierId = "soulful")
    )

    @Test
    fun loading() = capture("referral_loading", ReferralState())

    @Test
    fun error() = capture("referral_error", ReferralState(loadError = ReferralError.Network))

    @Test
    @Config(qualifiers = "w874dp-h402dp-xxhdpi")
    fun landscape() = capture("referral_landscape", ReferralState(program = previewProgram()))

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet() = capture(
        "referral_tablet",
        ReferralState(program = previewProgram(referrals = 12))
    )

    private fun capture(name: String, state: ReferralState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            OpalTheme {
                ReferralContent(state = state, onIntent = {}, onBack = {})
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

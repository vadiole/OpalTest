package vadiole.opaltest.feature.referral

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
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
    fun figma() = capture("referral_figma", ReferralState.Ready(previewProgram()))

    @Test
    fun proNoReferrals() = capture(
        "referral_pro_no_referrals",
        ReferralState.Ready(previewProgram(plan = Plan.Pro, referrals = 0))
    )

    @Test
    fun claiming() = capture(
        "referral_claiming",
        ReferralState.Ready(previewProgram(), claimingTierId = "soulful")
    )

    @Test
    fun loading() = capture("referral_loading", ReferralState.Loading)

    @Test
    fun error() = capture("referral_error", ReferralState.Failed(ReferralError.Network))

    @Test
    @Config(qualifiers = "w874dp-h402dp-xxhdpi")
    fun landscape() = capture("referral_landscape", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "w874dp-h402dp-xxhdpi")
    fun loadingLandscape() = capture("referral_loading_landscape", ReferralState.Loading)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tablet() = capture(
        "referral_tablet",
        ReferralState.Ready(previewProgram(referrals = 12))
    )

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun landscapeNarrow() = capture("referral_landscape_narrow", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun loadingLandscapeNarrow() = capture("referral_loading_landscape_narrow", ReferralState.Loading)

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun landscapeLongInviter() = capture(
        "referral_landscape_long_inviter",
        ReferralState.Ready(previewProgram().copy(invitedBy = LONG_INVITER))
    )

    @Test
    @Config(qualifiers = "w874dp-h330dp-xxhdpi")
    fun landscapeBarInsets() = captureFittingStartPane("referral_landscape_bar_insets")

    @Test
    @Config(qualifiers = "w915dp-h340dp-xxhdpi")
    fun landscapeWideBarInsets() = captureFittingStartPane("referral_landscape_wide_bar_insets")

    @Test
    @Config(qualifiers = "w732dp-h340dp-xxhdpi")
    fun landscapeNarrowBarInsets() = captureFittingStartPane("referral_landscape_narrow_bar_insets")

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun loadingTablet() = capture("referral_loading_tablet", ReferralState.Loading)

    @Test
    fun longInviter() = capture(
        "referral_long_inviter",
        ReferralState.Ready(previewProgram().copy(invitedBy = LONG_INVITER))
    )

    @Test
    fun longCode() = capture(
        "referral_long_code",
        ReferralState.Ready(previewProgram().copy(code = LONG_CODE))
    )

    @Test
    @Config(qualifiers = "w320dp-h1861dp-xxhdpi", fontScale = 2f)
    fun narrowLargeFont() = capture("referral_narrow_large_font", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "fr-w402dp-h1861dp-xxhdpi")
    fun french() = capture("referral_fr", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "es-w402dp-h1861dp-xxhdpi")
    fun spanish() = capture("referral_es", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "fr-w874dp-h402dp-xxhdpi")
    fun frenchLandscape() = capture("referral_fr_landscape", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "es-w732dp-h412dp-xxhdpi")
    fun spanishLandscapeNarrow() = capture("referral_es_landscape_narrow", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "fr-w360dp-h1861dp-xxhdpi", fontScale = 1.5f)
    fun frenchLargeFont() = capture("referral_fr_font_1_5", ReferralState.Ready(previewProgram(plan = Plan.Pro)))

    @Test
    @Config(qualifiers = "es-w360dp-h1861dp-xxhdpi", fontScale = 1.5f)
    fun spanishLargeFont() = capture("referral_es_font_1_5", ReferralState.Ready(previewProgram()))

    @Test
    @Config(qualifiers = "fr-w402dp-h874dp-xxhdpi")
    fun frenchError() = capture("referral_fr_error", ReferralState.Failed(ReferralError.Network))

    @Test
    @Config(qualifiers = "ar-w402dp-h874dp-xxhdpi")
    fun rtl() = capture("referral_rtl", ReferralState.Ready(previewProgram()))

    private fun captureFittingStartPane(name: String) {
        capture(name, ReferralState.Ready(previewProgram()))
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy)).assertCountEquals(1)
    }

    private fun capture(name: String, state: ReferralState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                OpalTheme {
                    ReferralContent(state = state, onIntent = {}, onBack = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val LONG_INVITER = "Maximilian Alexander von Hohenzollern-Sigmaringen"
        const val LONG_CODE = "ABCDEFGH23"
    }
}

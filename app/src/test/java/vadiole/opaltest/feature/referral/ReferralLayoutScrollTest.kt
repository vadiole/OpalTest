package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h900dp-mdpi")
class ReferralLayoutScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private var window by mutableStateOf(PHONE)

    @Test
    fun keepsTierOnTopAcrossOneAndTwoPanes() {
        compose.setContent { Screen() }
        scrollPane(containing = "Loyal MileStone", toKey = "soulful")
        val top = titleTop("Soulful MileStone")

        listOf(TABLET, LANDSCAPE, PHONE).forEach { size ->
            window = size
            assertEquals("at $size", top, titleTop("Soulful MileStone"), 1f)
        }
    }

    @Test
    fun carriesTwoPaneScrollBackToSingleList() {
        window = LANDSCAPE
        compose.setContent { Screen() }
        scrollPane(containing = "Loyal MileStone", toKey = "popular")
        val top = titleTop("Popular MileStone")

        window = PHONE
        assertEquals(top, titleTop("Popular MileStone"), 1f)
    }

    @Test
    fun keepsTierOnTopWhenRotationRecreatesTheScreen() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Screen() }
        scrollPane(containing = "Loyal MileStone", toKey = "soulful")
        val top = titleTop("Soulful MileStone")

        window = LANDSCAPE
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(top, titleTop("Soulful MileStone"), 1f)
    }

    @Composable
    private fun Screen() {
        val density = LocalDensity.current
        val windowInfo = remember(density) {
            object : WindowInfo {
                override val isWindowFocused = true
                override val containerSize: IntSize
                    get() = with(density) { IntSize(window.width.roundToPx(), window.height.roundToPx()) }
            }
        }
        CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
            OpalTheme {
                Box(Modifier.fillMaxSize()) {
                    ReferralContent(
                        state = ReferralState.Ready(previewProgram()),
                        onIntent = {},
                        onBack = {},
                        modifier = Modifier.size(window)
                    )
                }
            }
        }
    }

    private fun scrollPane(containing: String, toKey: String) {
        compose.onNode(hasScrollToKeyAction() and hasAnyDescendant(hasText(containing)), useUnmergedTree = true)
            .performScrollToKey(toKey)
    }

    private fun titleTop(title: String): Float =
        compose.onNodeWithText(title, useUnmergedTree = true).getUnclippedBoundsInRoot().top.value

    private companion object {
        val PHONE = DpSize(402.dp, 600.dp)
        val TABLET = DpSize(1280.dp, 800.dp)
        val LANDSCAPE = DpSize(874.dp, 402.dp)
    }
}

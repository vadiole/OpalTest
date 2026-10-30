package vadiole.opaltest.feature.referral

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
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
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.RevealContent
import vadiole.opaltest.feature.claimreveal.RevealPhase
import vadiole.opaltest.feature.claimreveal.rememberClaimRevealState
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class ReferralContentTest {
    @get:Rule
    val compose = createComposeRule()

    private var state: ReferralState by mutableStateOf(ReferralState.Loading)
    private lateinit var reveal: ClaimRevealState

    private fun setContent(initial: ReferralState) {
        state = initial
        compose.setContent {
            reveal = rememberClaimRevealState()
            OpalTheme {
                ReferralContent(state = state, onIntent = {}, onBack = {}, reveal = reveal)
            }
        }
        compose.waitForIdle()
    }

    private fun frame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private val claimButton = hasClickAction() and (hasText("Claim") or hasContentDescription("Claim"))

    private fun busy(node: SemanticsNode) = node.config.getOrNull(SemanticsProperties.StateDescription) == "Loading"

    private fun assertClaimNeverIdle(moment: String) {
        compose.onAllNodes(claimButton).fetchSemanticsNodes().forEach {
            assertTrue("Claim is idle while the reveal holds the gem ($moment)", busy(it))
        }
    }

    @Test
    fun `claim stays busy while the reveal holds the gem and through its exit`() {
        setContent(ReferralState.Ready(previewProgram(referrals = 1)))
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            reveal.open(
                MilestoneStone.Loyal,
                RevealContent(gem = R.drawable.img_milestone_loyal_large, title = "Loyal")
            )
        }
        var sawBusy = false
        val deadline = System.currentTimeMillis() + 20_000
        while (reveal.phase != RevealPhase.Revealed) {
            check(System.currentTimeMillis() < deadline) { "reveal never settled" }
            Thread.sleep(5)
            frame()
            assertClaimNeverIdle("opening")
            sawBusy = sawBusy || compose.onAllNodes(claimButton).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(sawBusy)

        compose.runOnIdle { reveal.close() }
        repeat(600) {
            frame()
            assertClaimNeverIdle("closing frame $it")
            if (reveal.activeKey == null) {
                repeat(60) { frame() }
                assertTrue(compose.onAllNodes(claimButton).fetchSemanticsNodes().isEmpty())
                return
            }
        }
        error("reveal never closed")
    }

    @Test
    @Config(qualifiers = "w402dp-h1861dp-xxhdpi")
    fun `tier card reads as one item with progress in friends and a separate claim`() {
        setContent(ReferralState.Ready(previewProgram(referrals = 5)))

        val card = compose.onNode(hasText("Popular MileStone"), useUnmergedTree = false)
        val texts = card.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertEquals(listOf("10 Friends", "Popular MileStone", "Reveal this special MileStone"), texts)
        assertEquals("5 of 10 friends", card.stateDescription())

        val claims = compose.onAllNodes(claimButton).fetchSemanticsNodes()
        val claimCard = compose.onNode(hasText("1 Year of Opal Pro")).fetchSemanticsNode()
        assertEquals(2, claims.size)
        assertFalse(claims.any { it.id == claimCard.id })
        assertFalse(claimCard.config[SemanticsProperties.Text].any { it.text == "Claim" })
    }

    @Test
    fun `large counts are grouped`() {
        setContent(ReferralState.Ready(previewProgram(referrals = 12_345)))

        compose.onNode(hasText("12,345 friends", substring = true)).assertExists()
    }

    @Test
    fun `retry shows progress right away`() {
        setContent(ReferralState.Failed(ReferralError.Network))
        val retry = compose.onNode(hasClickAction() and (hasText("Try again") or hasContentDescription("Try again")))
        assertEquals(null, retry.stateDescription())

        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state = ReferralState.Loading }
        repeat(2) { frame() }
        assertEquals("Loading", retry.stateDescription())
    }

    @Test
    fun `back is the first screen reader stop`() {
        setContent(ReferralState.Ready(previewProgram()))
        val root = compose.onRoot().fetchSemanticsNode()
        val view = root.root as View
        root.root!!.forceAccessibilityForTesting(true)
        compose.waitForIdle()

        val provider = view.accessibilityNodeProvider!!
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }).fetchSemanticsNodes()
        val next = nodes.associate { node ->
            val virtualId = if (node.id == root.id) View.NO_ID else node.id
            val info = provider.createAccessibilityNodeInfo(virtualId)
            node.id to info?.extras?.getInt(TRAVERSAL_BEFORE, -1)
        }.filterValues { it != null && it != -1 }
        val head = next.keys.single { it !in next.values }
        val order = generateSequence(head) { next[it] }.toList()
        val spoken = order.mapNotNull { id ->
            val node = nodes.first { it.id == id }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        }
        assertEquals("Back", spoken.first())
    }

    private fun SemanticsNodeInteraction.stateDescription(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private companion object {
        const val TRAVERSAL_BEFORE = "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
    }
}

package vadiole.opaltest.feature.referral

import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.RewardTier
import vadiole.opaltest.domain.referral.TierStatus
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
    private val intents = mutableListOf<ReferralIntent>()
    private lateinit var focusManager: FocusManager
    private lateinit var inputMode: InputModeManager

    private fun setContent(initial: ReferralState, probe: RewardTier? = null) {
        state = initial
        compose.setContent {
            reveal = rememberClaimRevealState()
            focusManager = LocalFocusManager.current
            inputMode = LocalInputModeManager.current
            OpalTheme {
                Column {
                    ReferralContent(
                        state = state,
                        onIntent = { intents += it },
                        onBack = {},
                        modifier = Modifier.weight(1f),
                        reveal = reveal
                    )
                    probe?.let {
                        TierCard(
                            tier = it,
                            successfulReferrals = 1,
                            claimAction = ClaimAction.Idle,
                            active = false,
                            reveal = reveal,
                            onClaim = {}
                        )
                    }
                }
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
        val program = previewProgram(referrals = 1)
        setContent(ReferralState.Ready(program), probe = program.tiers.first { it.id == "loyal" })
        assertTrue(compose.onAllNodes(claimButton).fetchSemanticsNodes().isEmpty())
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            reveal.open(
                MilestoneStone.Loyal,
                RevealContent(gem = R.drawable.img_milestone_loyal_large, title = R.string.referral_milestone_loyal)
            )
        }
        var sawBusy = false
        val deadline = System.currentTimeMillis() + 20_000
        while (reveal.phase != RevealPhase.Revealed) {
            check(System.currentTimeMillis() < deadline) { "reveal never settled" }
            Thread.sleep(5)
            frame()
            assertClaimNeverIdle("opening")
            sawBusy = sawBusy || compose.onAllNodes(claimButton).fetchSemanticsNodes().any(::busy)
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

    private fun liveRegions() =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).fetchSemanticsNodes()

    private fun announcement(): String? =
        liveRegions().single().config.getOrNull(SemanticsProperties.ContentDescription)?.single()

    private fun claim(program: ReferralProgram, tierId: String) = program.copy(
        tiers = program.tiers.map { if (it.id == tierId) it.copy(status = TierStatus.Claimed) else it }
    )

    @Test
    fun `claiming the free year announces it once through one status node that then clears`() {
        val program = previewProgram(referrals = 5)
        setContent(ReferralState.Ready(program, claimingTierId = "year"))
        assertEquals(null, announcement())

        compose.runOnIdle { state = ReferralState.Ready(claim(program, "year")) }
        compose.waitForIdle()

        assertEquals(1, liveRegions().size)
        assertEquals("1 Year of Opal Pro claimed", announcement())

        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(null, announcement())
    }

    @Test
    fun `rewards already claimed on first show are not announced`() {
        setContent(ReferralState.Ready(claim(previewProgram(referrals = 5), "year")))

        assertEquals(null, announcement())
    }

    @Test
    fun `a milestone is not announced again after its reveal`() {
        val program = previewProgram()
        setContent(ReferralState.Ready(program))
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            reveal.open(
                MilestoneStone.Soulful,
                RevealContent(gem = R.drawable.img_milestone_soulful_large, title = R.string.referral_milestone_soulful)
            )
            state = ReferralState.Ready(claim(program, "soulful"))
        }
        val deadline = System.currentTimeMillis() + 20_000
        while (reveal.phase != RevealPhase.Revealed) {
            check(System.currentTimeMillis() < deadline) { "reveal never settled" }
            Thread.sleep(5)
            frame()
        }
        assertEquals(null, announcement())

        compose.runOnIdle { reveal.close() }
        while (reveal.phase != RevealPhase.Landed) {
            check(System.currentTimeMillis() < deadline) { "reveal never landed" }
            frame()
            assertEquals(null, announcement())
        }
        repeat(30) { frame() }
        assertEquals(null, announcement())
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
    fun `invite card shows the inviter`() {
        setContent(ReferralState.Ready(previewProgram()))

        compose.onNode(hasText("Invited by Kenneth")).assertExists()
    }

    @Test
    fun `invite card reads as one item`() {
        setContent(ReferralState.Ready(previewProgram()))

        val card = compose.onNode(hasText("Invited by Kenneth")).fetchSemanticsNode()
        assertEquals(listOf("Opal"), card.config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("Invited by Kenneth"), card.config[SemanticsProperties.Text].map { it.text })
        assertInviteTextMergedIntoCard()
    }

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun `compact invite card reads as one item`() {
        setContent(ReferralState.Ready(previewProgram()))

        assertInviteTextMergedIntoCard()
    }

    private fun assertInviteTextMergedIntoCard() {
        val merged = compose.onNode(hasText("Invited by Kenneth")).fetchSemanticsNode()
        val text = compose.onNode(hasText("Invited by Kenneth"), useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(merged.id != text.id)
        assertTrue(merged.config.isMergingSemanticsOfDescendants)
    }

    @Test
    fun `no inviter hides the invite card`() {
        setContent(ReferralState.Ready(previewProgram().copy(invitedBy = null)))

        compose.onNode(hasText("Invited by", substring = true)).assertDoesNotExist()
        compose.onNode(hasText("Gift 30 days of Opal PRO, for free!")).assertExists()
    }

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun `no inviter hides the compact invite card`() {
        setContent(ReferralState.Ready(previewProgram().copy(invitedBy = null)))

        compose.onNode(hasText("Invited by", substring = true)).assertDoesNotExist()
        compose.onNode(hasText("Loyal MileStone")).assertExists()
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
        shadowOf(view.context.getSystemService(AccessibilityManager::class.java)).apply {
            setEnabled(true)
            setTouchExplorationEnabled(true)
        }
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

    @Test
    fun `code exposes one copy action and still copies on tap and long press`() {
        setContent(ReferralState.Ready(previewProgram()))
        val code = compose.onNode(hasContentDescription("Your Opal code", substring = true))
        val config = code.fetchSemanticsNode().config

        assertEquals("Copy invite", config[SemanticsActions.OnClick].label)
        assertFalse(config.contains(SemanticsActions.OnLongClick))
        code.performClick()
        code.performTouchInput { longClick() }
        assertEquals(listOf(ReferralIntent.CopyInvite, ReferralIntent.CopyInvite), intents)
    }

    private fun assertTabReachesBackFirst() {
        setContent(ReferralState.Ready(previewProgram()))
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithContentDescription("Back").assertIsFocused()
        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        compose.onNodeWithContentDescription("Your Opal code", substring = true).assertIsFocused()
    }

    @Test
    fun `back is the first keyboard tab stop`() = assertTabReachesBackFirst()

    @Test
    @Config(qualifiers = "w732dp-h412dp-xxhdpi")
    fun `back is the first keyboard tab stop in the short two-pane layout`() = assertTabReachesBackFirst()

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun `back is the first keyboard tab stop on a tablet`() = assertTabReachesBackFirst()

    private fun SemanticsNodeInteraction.stateDescription(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private companion object {
        const val TRAVERSAL_BEFORE = "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
    }
}

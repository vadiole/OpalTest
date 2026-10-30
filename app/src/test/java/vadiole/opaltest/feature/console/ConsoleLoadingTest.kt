package vadiole.opaltest.feature.console

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import vadiole.opaltest.R
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w402dp-h874dp")
class ConsoleLoadingTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()
    private val skeleton = context.getString(R.string.console_loading)
    private val error = context.getString(R.string.console_load_error)
    private val ready = ConsoleState(
        ConsoleLoad.Ready(
            ReferralDebugState(
                plan = Plan.Free,
                successfulReferrals = 3,
                claimedTiers = 1,
                nextTierAt = 5,
                latency = SimulatedLatency.None,
                failNextLoad = false,
                failNextClaim = false
            )
        )
    )

    private var state by mutableStateOf(ConsoleState())

    private fun setUp(initial: ConsoleState) {
        state = initial
        compose.mainClock.autoAdvance = false
        compose.setContent { OpalTheme { ConsoleContent(state = state, onIntent = {}) } }
    }

    @Test
    fun `fast cold start never shows the skeleton`() {
        setUp(ConsoleState())
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithContentDescription(skeleton).assertDoesNotExist()

        state = ready
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithContentDescription(skeleton).assertDoesNotExist()
    }

    @Test
    fun `retry keeps the error until the loading delay passes`() {
        setUp(ConsoleState(ConsoleLoad.Failed))
        compose.mainClock.advanceTimeBy(1_000)

        state = ConsoleState()
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText(error).assertExists()
        compose.onNodeWithContentDescription(skeleton).assertDoesNotExist()

        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithContentDescription(skeleton).assertExists()
    }
}

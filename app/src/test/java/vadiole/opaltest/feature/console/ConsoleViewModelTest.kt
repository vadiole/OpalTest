package vadiole.opaltest.feature.console

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vadiole.opaltest.MainDispatcherRule
import vadiole.opaltest.data.debug.DebugOutcome
import vadiole.opaltest.data.debug.ReferralDebugControls
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError

@OptIn(ExperimentalCoroutinesApi::class)
class ConsoleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initial = ReferralDebugState(
        plan = Plan.Free,
        successfulReferrals = 3,
        maxReferrals = 999,
        claimedTiers = 1,
        nextTierAt = 5,
        latency = SimulatedLatency.Normal,
        failNextLoad = false,
        failNextClaim = false
    )

    private inner class FakeControls(var failingReads: Int = 0, var failWrites: Boolean = false) :
        ReferralDebugControls {
        val data = MutableStateFlow(initial)
        var resets = 0
        val failNextClaimWrites = mutableListOf<Boolean>()
        val referralWrites = mutableListOf<Int>()

        override val state: Flow<DebugOutcome<ReferralDebugState>> = flow {
            if (failingReads > 0) {
                failingReads--
                emit(Outcome.Failure(ReferralError.Network))
            } else {
                emitAll(data.map { Outcome.Success(it) })
            }
        }

        private fun write(block: () -> Unit): DebugOutcome<Unit> = if (failWrites) {
            Outcome.Failure(ReferralError.Network)
        } else {
            Outcome.Success(block())
        }

        override suspend fun setPlan(plan: Plan) = write {}

        override suspend fun setReferrals(count: Int) = write { referralWrites += count }

        override suspend fun advanceToNextTier() = write {}

        override suspend fun setLatency(latency: SimulatedLatency) = write {}

        override suspend fun setFailNextLoad(fail: Boolean) = write {}

        override suspend fun setFailNextClaim(fail: Boolean) = write {
            failNextClaimWrites += fail
            data.value = data.value.copy(failNextClaim = fail)
        }

        override suspend fun reset() = write { resets++ }
    }

    private val ConsoleViewModel.isConfirmingReset
        get() = (state.value.load as? ConsoleLoad.Ready)?.isConfirmingReset == true

    @Test
    fun `load failure shows error and retry loads content`() = runTest {
        val viewModel = ConsoleViewModel(FakeControls(failingReads = 1))
        assertEquals(ConsoleLoad.Loading, viewModel.state.value.load)

        runCurrent()
        assertEquals(ConsoleLoad.Failed, viewModel.state.value.load)

        viewModel.onIntent(ConsoleIntent.Retry)
        assertEquals(ConsoleLoad.Loading, viewModel.state.value.load)

        runCurrent()
        assertEquals(ConsoleState(ConsoleLoad.Ready(initial)), viewModel.state.value)
    }

    @Test
    fun `add friend is a no-op at the referral cap`() = runTest {
        val controls = FakeControls()
        controls.data.value = initial.copy(successfulReferrals = initial.maxReferrals)
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(emptyList<Int>(), controls.referralWrites)

        controls.data.value = initial
        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(listOf(initial.successfulReferrals + 1), controls.referralWrites)
    }

    @Test
    fun `reset waits for a second tap`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.Reset())
        runCurrent()
        assertTrue(viewModel.isConfirmingReset)
        assertEquals(0, controls.resets)

        viewModel.onIntent(ConsoleIntent.Reset())
        runCurrent()
        assertFalse(viewModel.isConfirmingReset)
        assertEquals(1, controls.resets)

        advanceTimeBy(RESET_CONFIRM_TIMEOUT * 2)
        assertFalse(viewModel.isConfirmingReset)
    }

    @Test
    fun `reset confirmation expires`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.Reset())
        advanceTimeBy(RESET_CONFIRM_TIMEOUT)
        runCurrent()
        assertFalse(viewModel.isConfirmingReset)

        viewModel.onIntent(ConsoleIntent.Reset())
        runCurrent()
        assertTrue(viewModel.isConfirmingReset)
        assertEquals(0, controls.resets)
    }

    @Test
    fun `reset confirmation honours the requested timeout`() = runTest {
        val viewModel = ConsoleViewModel(FakeControls())
        runCurrent()
        val timeout = RESET_CONFIRM_TIMEOUT * 3

        viewModel.onIntent(ConsoleIntent.Reset(timeout))
        advanceTimeBy(RESET_CONFIRM_TIMEOUT * 2)
        runCurrent()
        assertTrue(viewModel.isConfirmingReset)

        advanceTimeBy(RESET_CONFIRM_TIMEOUT)
        runCurrent()
        assertFalse(viewModel.isConfirmingReset)
    }

    @Test
    fun `reset is ignored until the state loads`() = runTest {
        val controls = FakeControls(failingReads = 1)
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.Reset())
        runCurrent()
        assertEquals(ConsoleLoad.Failed, viewModel.state.value.load)
        assertEquals(0, controls.resets)
    }

    @Test
    fun `fail next claim toggles from the saved state`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.ToggleFailNextClaim)
        runCurrent()
        assertEquals(listOf(true), controls.failNextClaimWrites)
        assertTrue((viewModel.state.value.load as ConsoleLoad.Ready).debug.failNextClaim)

        viewModel.onIntent(ConsoleIntent.ToggleFailNextClaim)
        runCurrent()
        assertEquals(listOf(true, false), controls.failNextClaimWrites)
        assertFalse((viewModel.state.value.load as ConsoleLoad.Ready).debug.failNextClaim)
    }

    @Test
    fun `adding the friend that reaches the next tier sends tier reached`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        val threshold = checkNotNull(initial.nextTierAt)
        val effects = mutableListOf<ConsoleEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        runCurrent()

        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(listOf(ConsoleEffect.FriendAdded), effects)

        controls.data.value = initial.copy(successfulReferrals = threshold - 1)
        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(listOf(ConsoleEffect.FriendAdded, ConsoleEffect.TierReached), effects)

        controls.data.value = initial.copy(successfulReferrals = threshold, nextTierAt = null)
        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(
            listOf(ConsoleEffect.FriendAdded, ConsoleEffect.TierReached, ConsoleEffect.FriendAdded),
            effects
        )
    }

    @Test
    fun `failed write sends no effect`() = runTest {
        val controls = FakeControls(failWrites = true)
        val viewModel = ConsoleViewModel(controls)
        val effects = mutableListOf<ConsoleEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        runCurrent()

        viewModel.onIntent(ConsoleIntent.AddFriend)
        viewModel.onIntent(ConsoleIntent.AdvanceToNextTier)
        runCurrent()
        assertEquals(emptyList<ConsoleEffect>(), effects)

        controls.failWrites = false
        viewModel.onIntent(ConsoleIntent.AddFriend)
        runCurrent()
        assertEquals(listOf(ConsoleEffect.FriendAdded), effects)
    }
}

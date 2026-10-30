package vadiole.opaltest.feature.console

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vadiole.opaltest.MainDispatcherRule
import vadiole.opaltest.data.debug.ReferralDebugControls
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

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

    private inner class FakeControls(var failingReads: Int = 0) : ReferralDebugControls {
        val data = MutableStateFlow(initial)
        var resets = 0
        val failNextClaimWrites = mutableListOf<Boolean>()
        val referralWrites = mutableListOf<Int>()

        override val state: Flow<ReferralDebugState> = flow {
            if (failingReads > 0) {
                failingReads--
                throw IOException()
            }
            emitAll(data)
        }

        override suspend fun setPlan(plan: Plan) = Unit

        override suspend fun setReferrals(count: Int) {
            referralWrites += count
        }

        override suspend fun advanceToNextTier() = Unit

        override suspend fun setLatency(latency: SimulatedLatency) = Unit

        override suspend fun setFailNextLoad(fail: Boolean) = Unit

        override suspend fun setFailNextClaim(fail: Boolean) {
            failNextClaimWrites += fail
            data.value = data.value.copy(failNextClaim = fail)
        }

        override suspend fun reset() {
            resets++
        }
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
}

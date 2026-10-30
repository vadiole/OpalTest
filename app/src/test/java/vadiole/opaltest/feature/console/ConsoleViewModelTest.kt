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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vadiole.opaltest.MainDispatcherRule
import vadiole.opaltest.data.fake.ReferralDebugControls
import vadiole.opaltest.data.fake.ReferralDebugState
import vadiole.opaltest.data.fake.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

@OptIn(ExperimentalCoroutinesApi::class)
class ConsoleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initial = ReferralDebugState(
        plan = Plan.Free,
        successfulReferrals = 3,
        claimedTiers = 1,
        nextTierAt = 5,
        latency = SimulatedLatency.Normal,
        failNextLoad = false,
        failNextClaim = false
    )

    private inner class FakeControls(var failingReads: Int = 0) : ReferralDebugControls {
        val data = MutableStateFlow(initial)
        var resets = 0

        override val state: Flow<ReferralDebugState> = flow {
            if (failingReads > 0) {
                failingReads--
                throw IOException()
            }
            emitAll(data)
        }

        override suspend fun setPlan(plan: Plan) = Unit

        override suspend fun setReferrals(count: Int) = Unit

        override suspend fun advanceToNextTier() = Unit

        override suspend fun setLatency(latency: SimulatedLatency) = Unit

        override suspend fun setFailNextLoad(fail: Boolean) = Unit

        override suspend fun setFailNextClaim(fail: Boolean) = Unit

        override suspend fun reset() {
            resets++
        }
    }

    @Test
    fun `load failure shows error and retry loads content`() = runTest {
        val viewModel = ConsoleViewModel(FakeControls(failingReads = 1))
        assertTrue(viewModel.state.value.isLoading)

        runCurrent()
        assertTrue(viewModel.state.value.loadFailed)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.debug)

        viewModel.onIntent(ConsoleIntent.Retry)
        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.loadFailed)

        runCurrent()
        assertEquals(ConsoleState(debug = initial, isLoading = false), viewModel.state.value)
    }

    @Test
    fun `reset waits for a second tap`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.Reset)
        runCurrent()
        assertTrue(viewModel.state.value.isConfirmingReset)
        assertEquals(0, controls.resets)

        viewModel.onIntent(ConsoleIntent.Reset)
        runCurrent()
        assertFalse(viewModel.state.value.isConfirmingReset)
        assertEquals(1, controls.resets)

        advanceTimeBy(ConsoleViewModel.RESET_CONFIRM_TIMEOUT * 2)
        assertFalse(viewModel.state.value.isConfirmingReset)
    }

    @Test
    fun `reset confirmation expires`() = runTest {
        val controls = FakeControls()
        val viewModel = ConsoleViewModel(controls)
        runCurrent()

        viewModel.onIntent(ConsoleIntent.Reset)
        advanceTimeBy(ConsoleViewModel.RESET_CONFIRM_TIMEOUT)
        runCurrent()
        assertFalse(viewModel.state.value.isConfirmingReset)

        viewModel.onIntent(ConsoleIntent.Reset)
        runCurrent()
        assertTrue(viewModel.state.value.isConfirmingReset)
        assertEquals(0, controls.resets)
    }
}

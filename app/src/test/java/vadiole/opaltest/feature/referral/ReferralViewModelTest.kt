package vadiole.opaltest.feature.referral

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import vadiole.opaltest.MainDispatcherRule
import vadiole.opaltest.data.referral.ReferralRepository
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun program(referrals: Int) = ReferralProgram(
        code = "CODE",
        shareUrl = "https://opal.so/r/CODE",
        plan = Plan.Free,
        successfulReferrals = referrals,
        inviteeTrialDays = 14,
        invitedBy = null,
        tiers = emptyList()
    )

    private class FakeRepository : ReferralRepository {
        val loads = ArrayDeque<CompletableDeferred<Outcome<ReferralProgram, ReferralError>>>()
        val claims = ArrayDeque<CompletableDeferred<Outcome<ReferralProgram, ReferralError>>>()

        override suspend fun getProgram() = CompletableDeferred<Outcome<ReferralProgram, ReferralError>>()
            .also { loads.addLast(it) }
            .await()

        override suspend fun claimReward(tierId: String) =
            CompletableDeferred<Outcome<ReferralProgram, ReferralError>>()
                .also { claims.addLast(it) }
                .await()
    }

    @Test
    fun `failed reload after rejected claim is reported and next success clears it`() = runTest {
        val repository = FakeRepository()
        val viewModel = ReferralViewModel(repository)
        runCurrent()
        repository.loads.removeFirst().complete(Outcome.Success(program(1)))
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.AlreadyClaimed))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.AlreadyClaimed), awaitItem())

            repository.loads.removeFirst().complete(Outcome.Failure(ReferralError.Network))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.Network), awaitItem())
            assertEquals(ReferralError.Network, viewModel.state.value.loadError)
            assertEquals(program(1), viewModel.state.value.program)

            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.AlreadyClaimed))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.AlreadyClaimed), awaitItem())

            repository.loads.removeFirst().complete(Outcome.Success(program(2)))
            runCurrent()
            assertEquals(program(2), viewModel.state.value.program)
            assertNull(viewModel.state.value.loadError)
            expectNoEvents()
        }
    }

    @Test
    fun `stale reload cannot overwrite a newer claim result`() = runTest {
        val repository = FakeRepository()
        val viewModel = ReferralViewModel(repository)
        runCurrent()
        repository.loads.removeFirst().complete(Outcome.Success(program(1)))
        runCurrent()

        viewModel.onIntent(ReferralIntent.Claim("first"))
        runCurrent()
        repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.NotEligible))
        runCurrent()
        val staleLoad = repository.loads.removeFirst()

        viewModel.onIntent(ReferralIntent.Claim("second"))
        runCurrent()
        repository.claims.removeFirst().complete(Outcome.Success(program(3)))
        runCurrent()

        staleLoad.complete(Outcome.Success(program(2)))
        runCurrent()
        assertEquals(program(3), viewModel.state.value.program)
        assertNull(viewModel.state.value.claimingTierId)
    }
}

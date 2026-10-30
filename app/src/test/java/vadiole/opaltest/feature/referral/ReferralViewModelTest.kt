package vadiole.opaltest.feature.referral

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vadiole.opaltest.MainDispatcherRule
import vadiole.opaltest.data.referral.ReferralRepository
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.RewardTier
import vadiole.opaltest.domain.referral.TierStatus

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
        tiers = listOf("tier", "first", "second").map {
            RewardTier(it, 1, Reward.Milestone(MilestoneStone.Loyal), TierStatus.Claimable)
        }
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

    private fun TestScope.loadedViewModel(repository: FakeRepository, program: ReferralProgram = program(1)) =
        ReferralViewModel(repository).also {
            runCurrent()
            repository.loads.removeFirst().complete(Outcome.Success(program))
            runCurrent()
        }

    @Test
    fun `initial load shows the program`() = runTest {
        val repository = FakeRepository()
        val viewModel = ReferralViewModel(repository)
        runCurrent()
        assertEquals(ReferralState.Loading, viewModel.state.value)

        repository.loads.removeFirst().complete(Outcome.Success(program(1)))
        runCurrent()
        assertEquals(ReferralState.Ready(program(1)), viewModel.state.value)
    }

    @Test
    fun `failed load shows the error and retry loads again`() = runTest {
        val repository = FakeRepository()
        val viewModel = ReferralViewModel(repository)
        runCurrent()
        repository.loads.removeFirst().complete(Outcome.Failure(ReferralError.Network))
        runCurrent()
        assertEquals(ReferralState.Failed(ReferralError.Network), viewModel.state.value)

        viewModel.onIntent(ReferralIntent.Retry)
        runCurrent()
        assertEquals(ReferralState.Loading, viewModel.state.value)

        repository.loads.removeFirst().complete(Outcome.Success(program(2)))
        runCurrent()
        assertEquals(ReferralState.Ready(program(2)), viewModel.state.value)
    }

    @Test
    fun `successful claim replaces the program and reports the reward`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            assertEquals(ReferralState.Ready(program(1), claimingTierId = "tier"), viewModel.state.value)

            viewModel.onIntent(ReferralIntent.Claim("first"))
            runCurrent()
            assertEquals(1, repository.claims.size)

            repository.claims.removeFirst().complete(Outcome.Success(program(2)))
            runCurrent()
            assertEquals(ReferralState.Ready(program(2)), viewModel.state.value)
            assertEquals(ReferralEffect.ClaimSucceeded(Reward.Milestone(MilestoneStone.Loyal)), awaitItem())
        }
    }

    @Test
    fun `claiming a tier that is already claimed is ignored`() = runTest {
        val repository = FakeRepository()
        val claimed = program(1).let { p ->
            p.copy(tiers = p.tiers.map { if (it.id == "tier") it.copy(status = TierStatus.Claimed) else it })
        }
        val viewModel = loadedViewModel(repository, claimed)

        viewModel.onIntent(ReferralIntent.Claim("tier"))
        runCurrent()

        assertTrue(repository.claims.isEmpty())
        assertEquals(ReferralState.Ready(claimed), viewModel.state.value)
    }

    @Test
    fun `failed claim re-enables claiming without reloading`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.Network))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.Network), awaitItem())
            assertEquals(ReferralState.Ready(program(1)), viewModel.state.value)
            assertTrue(repository.loads.isEmpty())
        }
    }

    @Test
    fun `failed reload after rejected claim keeps the program silently`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.AlreadyClaimed))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.AlreadyClaimed), awaitItem())

            repository.loads.removeFirst().complete(Outcome.Failure(ReferralError.Network))
            runCurrent()
            assertEquals(ReferralState.Ready(program(1)), viewModel.state.value)

            viewModel.onIntent(ReferralIntent.Claim("tier"))
            runCurrent()
            repository.claims.removeFirst().complete(Outcome.Failure(ReferralError.AlreadyClaimed))
            runCurrent()
            assertEquals(ReferralEffect.ClaimFailed(ReferralError.AlreadyClaimed), awaitItem())

            repository.loads.removeFirst().complete(Outcome.Success(program(2)))
            runCurrent()
            assertEquals(ReferralState.Ready(program(2)), viewModel.state.value)
            expectNoEvents()
        }
    }

    @Test
    fun `share ignores repeats until the cooldown ends`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(repository)
        val share = ReferralEffect.Share(Invite("CODE", "https://opal.so/r/CODE", 14))

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.Share)
            viewModel.onIntent(ReferralIntent.Share)
            runCurrent()
            assertEquals(share, awaitItem())
            expectNoEvents()

            advanceTimeBy(ReferralViewModel.SHARE_COOLDOWN)
            runCurrent()
            viewModel.onIntent(ReferralIntent.Share)
            runCurrent()
            assertEquals(share, awaitItem())
        }
    }

    @Test
    fun `copy carries the invite and is ignored before the program loads`() = runTest {
        val repository = FakeRepository()
        val viewModel = ReferralViewModel(repository)

        viewModel.effects.test {
            viewModel.onIntent(ReferralIntent.CopyInvite)
            runCurrent()
            expectNoEvents()

            repository.loads.removeFirst().complete(Outcome.Success(program(1)))
            runCurrent()
            viewModel.onIntent(ReferralIntent.CopyInvite)
            runCurrent()
            assertEquals(ReferralEffect.CopyInvite(Invite("CODE", "https://opal.so/r/CODE", 14)), awaitItem())
        }
    }

    @Test
    fun `stale reload cannot overwrite a newer claim result`() = runTest {
        val repository = FakeRepository()
        val viewModel = loadedViewModel(repository)

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
        assertEquals(ReferralState.Ready(program(3)), viewModel.state.value)
    }
}

package vadiole.opaltest.data.fake

import androidx.datastore.core.DataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import vadiole.opaltest.data.debug.DebugOutcome
import vadiole.opaltest.data.debug.ReferralDebugControls
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.data.referral.ReferralProgramDto
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError

class FakeReferralBackend(private val store: DataStore<FakeServerState>) : ReferralDebugControls {
    override val state: Flow<DebugOutcome<ReferralDebugState>> = store.data
        .map<FakeServerState, DebugOutcome<ReferralDebugState>> { state ->
            Outcome.Success(
                ReferralDebugState(
                    plan = state.plan.toPlan(),
                    successfulReferrals = state.successfulReferrals,
                    maxReferrals = FakeReferralRules.MAX_REFERRALS,
                    claimedTiers = state.claimedTierIds.size,
                    nextTierAt = FakeReferralRules.nextTierAt(state),
                    latency = state.latency,
                    failNextLoad = state.failNextLoad,
                    failNextClaim = state.failNextClaim
                )
            )
        }
        .catch { if (it is IOException) emit(Outcome.Failure(ReferralError.Network)) else throw it }

    suspend fun program(): ReferralProgramDto = FakeReferralRules.program(store.data.first())

    suspend fun claim(tierId: String): ReferralProgramDto = FakeReferralRules.program(
        store.updateData {
            FakeReferralRules.claim(it, tierId)
        }
    )

    suspend fun latency(): SimulatedLatency = store.data.first().latency

    suspend fun consumeFailure(call: FakeCall): Boolean {
        var fail = false
        store.updateData { state ->
            when (call) {
                FakeCall.Load -> state.copy(failNextLoad = false).also { fail = state.failNextLoad }
                FakeCall.Claim -> state.copy(failNextClaim = false).also { fail = state.failNextClaim }
            }
        }
        return fail
    }

    override suspend fun setPlan(plan: Plan) = update { it.copy(plan = plan.toServerPlan()) }

    override suspend fun setReferrals(count: Int) =
        update { it.copy(successfulReferrals = count.coerceIn(0, FakeReferralRules.MAX_REFERRALS)) }

    override suspend fun advanceToNextTier() =
        update { state -> FakeReferralRules.nextTierAt(state)?.let { state.copy(successfulReferrals = it) } ?: state }

    override suspend fun setLatency(latency: SimulatedLatency) = update { it.copy(latency = latency) }

    override suspend fun setFailNextLoad(fail: Boolean) = update { it.copy(failNextLoad = fail) }

    override suspend fun setFailNextClaim(fail: Boolean) = update { it.copy(failNextClaim = fail) }

    override suspend fun reset() = update { FakeServerStateSerializer.defaultValue }

    private suspend fun update(transform: (FakeServerState) -> FakeServerState): DebugOutcome<Unit> = try {
        store.updateData(transform)
        Outcome.Success(Unit)
    } catch (_: IOException) {
        Outcome.Failure(ReferralError.Network)
    }
}

enum class FakeCall { Load, Claim }

package vadiole.opaltest.data.fake

import kotlinx.coroutines.flow.Flow
import vadiole.opaltest.domain.referral.Plan

interface ReferralDebugControls {
    val state: Flow<ReferralDebugState>

    suspend fun setPlan(plan: Plan)

    suspend fun setReferrals(count: Int)

    suspend fun advanceToNextTier()

    suspend fun setLatency(latency: SimulatedLatency)

    suspend fun setFailNextLoad(fail: Boolean)

    suspend fun setFailNextClaim(fail: Boolean)

    suspend fun reset()
}

data class ReferralDebugState(
    val plan: Plan,
    val successfulReferrals: Int,
    val claimedTiers: Int,
    val nextTierAt: Int?,
    val latency: SimulatedLatency,
    val failNextLoad: Boolean,
    val failNextClaim: Boolean
)

package vadiole.opaltest.data.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError

typealias DebugOutcome<T> = Outcome<T, ReferralError>

interface ReferralDebugControls {
    val state: Flow<DebugOutcome<ReferralDebugState>>

    suspend fun setPlan(plan: Plan): DebugOutcome<Unit>

    suspend fun setReferrals(count: Int): DebugOutcome<Unit>

    suspend fun advanceToNextTier(): DebugOutcome<Unit>

    suspend fun setLatency(latency: SimulatedLatency): DebugOutcome<Unit>

    suspend fun setFailNextLoad(fail: Boolean): DebugOutcome<Unit>

    suspend fun setFailNextClaim(fail: Boolean): DebugOutcome<Unit>

    suspend fun reset(): DebugOutcome<Unit>
}

data class ReferralDebugState(
    val plan: Plan,
    val successfulReferrals: Int,
    val maxReferrals: Int,
    val claimedTiers: Int,
    val nextTierAt: Int?,
    val latency: SimulatedLatency,
    val failNextLoad: Boolean,
    val failNextClaim: Boolean
)

@Serializable
enum class SimulatedLatency(val millis: Long) {
    @SerialName("none")
    None(0),

    @SerialName("normal")
    Normal(800),

    @SerialName("slow")
    Slow(3000)
}

package vadiole.opaltest.feature.referral

import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram

data class ReferralState(
    val program: ReferralProgram? = null,
    val loadError: ReferralError? = null,
    val claimingTierId: String? = null
)

sealed interface ReferralIntent {
    data object Retry : ReferralIntent

    data object Share : ReferralIntent

    data class Claim(val tierId: String) : ReferralIntent
}

sealed interface ReferralEffect {
    data class Share(val code: String, val url: String, val trialDays: Int) : ReferralEffect

    data object ClaimSucceeded : ReferralEffect

    data class ClaimFailed(val error: ReferralError) : ReferralEffect
}

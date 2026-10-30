package vadiole.opaltest.feature.referral

import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.Reward

data class ReferralState(
    val program: ReferralProgram? = null,
    val loadError: ReferralError? = null,
    val claimingTierId: String? = null
)

sealed interface ReferralIntent {
    data object Retry : ReferralIntent

    data object Share : ReferralIntent

    data object CopyInvite : ReferralIntent

    data class Claim(val tierId: String) : ReferralIntent
}

data class Invite(val code: String, val url: String, val trialDays: Int)

sealed interface ReferralEffect {
    data class Share(val invite: Invite) : ReferralEffect

    data class CopyInvite(val invite: Invite) : ReferralEffect

    data class ClaimSucceeded(val reward: Reward) : ReferralEffect

    data class ClaimFailed(val error: ReferralError) : ReferralEffect
}

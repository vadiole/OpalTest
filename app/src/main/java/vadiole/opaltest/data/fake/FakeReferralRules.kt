package vadiole.opaltest.data.fake

import vadiole.opaltest.data.referral.ApiErrorCodes
import vadiole.opaltest.data.referral.ApiException
import vadiole.opaltest.data.referral.MoneyDto
import vadiole.opaltest.data.referral.ReferralProgramDto
import vadiole.opaltest.data.referral.RewardDto
import vadiole.opaltest.data.referral.RewardTierDto

object FakeReferralRules {
    const val REFERRAL_CODE = "FR333"
    const val MAX_REFERRALS = 999
    private const val INVITEE_TRIAL_DAYS = 30
    private const val INVITED_BY = "Kenneth"
    private const val FREE_YEAR_TIER_ID = "pro_one_year"

    data class TierDefinition(
        val id: String,
        val requiredReferrals: Int,
        val reward: RewardDto,
        val freeUsersOnly: Boolean = false
    )

    val catalog = listOf(
        TierDefinition("milestone_loyal", 1, RewardDto(type = "milestone", stone = "loyal")),
        TierDefinition("milestone_soulful", 3, RewardDto(type = "milestone", stone = "soulful")),
        TierDefinition(
            FREE_YEAR_TIER_ID,
            5,
            RewardDto(type = "pro_access", term = "one_year", fulfillment = "instant"),
            freeUsersOnly = true
        ),
        TierDefinition("milestone_popular", 10, RewardDto(type = "milestone", stone = "popular")),
        TierDefinition(
            "pro_two_years",
            20,
            RewardDto(
                type = "pro_access",
                term = "two_years",
                fulfillment = "contact_support",
                value = MoneyDto(19900, "USD")
            )
        ),
        TierDefinition(
            "pro_lifetime",
            50,
            RewardDto(
                type = "pro_access",
                term = "lifetime",
                fulfillment = "contact_support",
                value = MoneyDto(39900, "USD")
            )
        ),
        TierDefinition("mystery", 100, RewardDto(type = "mystery", fulfillment = "contact_support"))
    )

    fun program(state: FakeServerState) = ReferralProgramDto(
        referralCode = REFERRAL_CODE,
        shareUrl = "https://opal.so/invite/$REFERRAL_CODE",
        plan = if (state.plan == ServerPlan.Pro) "pro" else "free",
        successfulReferrals = state.successfulReferrals,
        inviteeTrialDays = INVITEE_TRIAL_DAYS,
        invitedBy = INVITED_BY,
        tiers = catalog.filter { it.isVisibleTo(state) }.map { tier ->
            RewardTierDto(
                id = tier.id,
                requiredReferrals = tier.requiredReferrals,
                status = when {
                    tier.id in state.claimedTierIds -> "claimed"
                    state.successfulReferrals >= tier.requiredReferrals -> "claimable"
                    else -> "locked"
                },
                reward = tier.reward
            )
        }
    )

    fun claim(state: FakeServerState, tierId: String): FakeServerState {
        val tier = catalog.find { it.id == tierId } ?: throw ApiException(404, ApiErrorCodes.UNKNOWN_TIER)
        when {
            tierId in state.claimedTierIds -> throw ApiException(409, ApiErrorCodes.ALREADY_CLAIMED)

            !tier.isVisibleTo(state) || state.successfulReferrals < tier.requiredReferrals ->
                throw ApiException(403, ApiErrorCodes.NOT_ELIGIBLE)
        }
        return state.copy(
            claimedTierIds = state.claimedTierIds + tierId,
            plan = if (tierId == FREE_YEAR_TIER_ID) ServerPlan.Pro else state.plan
        )
    }

    fun nextTierAt(state: FakeServerState): Int? = catalog
        .filter { it.isVisibleTo(state) }
        .map { it.requiredReferrals }
        .firstOrNull { it > state.successfulReferrals }

    private fun TierDefinition.isVisibleTo(state: FakeServerState) =
        !freeUsersOnly || state.plan == ServerPlan.Free || id in state.claimedTierIds
}

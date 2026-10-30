package vadiole.opaltest.data.referral

import java.util.Currency
import vadiole.opaltest.domain.referral.Fulfillment
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.Money
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ProTerm
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.RewardTier
import vadiole.opaltest.domain.referral.TierStatus

fun ReferralProgramDto.toDomain() = ReferralProgram(
    code = referralCode,
    shareUrl = shareUrl,
    plan = if (plan == "pro") Plan.Pro else Plan.Free,
    successfulReferrals = successfulReferrals.coerceAtLeast(0),
    inviteeTrialDays = inviteeTrialDays,
    invitedBy = invitedBy?.trim()?.takeIf { it.isNotEmpty() },
    tiers = tiers.mapNotNull { it.toDomain() }.distinctBy { it.id }.sortedBy { it.requiredReferrals }
)

private fun RewardTierDto.toDomain(): RewardTier? {
    if (requiredReferrals <= 0) return null
    return RewardTier(
        id = id,
        requiredReferrals = requiredReferrals,
        reward = reward.toDomain() ?: return null,
        status = when (status) {
            "claimed" -> TierStatus.Claimed
            "claimable" -> TierStatus.Claimable
            else -> TierStatus.Locked
        }
    )
}

private fun RewardDto.toDomain(): Reward? = when (type) {
    "milestone" -> when (stone) {
        "loyal" -> MilestoneStone.Loyal
        "soulful" -> MilestoneStone.Soulful
        "popular" -> MilestoneStone.Popular
        else -> null
    }?.let(Reward::Milestone)

    "pro_access" -> {
        val term = when (term) {
            "one_year" -> ProTerm.OneYear
            "two_years" -> ProTerm.TwoYears
            "lifetime" -> ProTerm.Lifetime
            else -> null
        }
        term?.let {
            Reward.ProAccess(
                term = it,
                fulfillment = if (fulfillment == "instant") Fulfillment.Instant else Fulfillment.ContactSupport,
                value = value?.toDomain()
            )
        }
    }

    "mystery" -> Reward.Mystery

    else -> null
}

private fun MoneyDto.toDomain(): Money? = runCatching { Currency.getInstance(currency) }
    .getOrNull()
    ?.takeIf { it.defaultFractionDigits >= 0 }
    ?.let { Money(amountMinor, it) }

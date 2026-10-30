package vadiole.opaltest.feature.referral

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

internal fun previewProgram(plan: Plan = Plan.Free, referrals: Int = 3): ReferralProgram {
    fun tier(id: String, required: Int, reward: Reward, claimed: Boolean = false) = RewardTier(
        id = id,
        requiredReferrals = required,
        reward = reward,
        status = when {
            claimed -> TierStatus.Claimed
            referrals >= required -> TierStatus.Claimable
            else -> TierStatus.Locked
        }
    )
    return ReferralProgram(
        code = "FR333",
        shareUrl = "https://opal.so/invite/FR333",
        plan = plan,
        successfulReferrals = referrals,
        inviteeTrialDays = 30,
        invitedBy = "Kenneth",
        tiers = listOfNotNull(
            tier("loyal", 1, Reward.Milestone(MilestoneStone.Loyal), claimed = referrals >= 1),
            tier("soulful", 3, Reward.Milestone(MilestoneStone.Soulful)),
            tier("year", 5, Reward.ProAccess(ProTerm.OneYear, Fulfillment.Instant, null))
                .takeIf { plan == Plan.Free },
            tier("popular", 10, Reward.Milestone(MilestoneStone.Popular)),
            tier(
                "two_years",
                20,
                Reward.ProAccess(
                    ProTerm.TwoYears,
                    Fulfillment.ContactSupport,
                    Money(19900, Currency.getInstance("USD"))
                )
            ),
            tier(
                "lifetime",
                50,
                Reward.ProAccess(
                    ProTerm.Lifetime,
                    Fulfillment.ContactSupport,
                    Money(39900, Currency.getInstance("USD"))
                )
            ),
            tier("mystery", 100, Reward.Mystery)
        )
    )
}

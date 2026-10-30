package vadiole.opaltest.domain.referral

data class ReferralProgram(
    val code: String,
    val shareUrl: String,
    val plan: Plan,
    val successfulReferrals: Int,
    val inviteeTrialDays: Int,
    val invitedBy: String?,
    val tiers: List<RewardTier>
)

enum class Plan { Free, Pro }

data class RewardTier(val id: String, val requiredReferrals: Int, val reward: Reward, val status: TierStatus)

enum class TierStatus { Locked, Claimable, Claimed }

sealed interface Reward {
    data class Milestone(val stone: MilestoneStone) : Reward

    data class ProAccess(val term: ProTerm, val fulfillment: Fulfillment, val value: Money?) : Reward

    data object Mystery : Reward
}

enum class MilestoneStone { Loyal, Soulful, Popular }

enum class ProTerm { OneYear, TwoYears, Lifetime }

enum class Fulfillment { Instant, ContactSupport }

data class Money(val amountMinor: Long, val currencyCode: String)

enum class ReferralError { Network, NotEligible, AlreadyClaimed, Unknown }

fun RewardTier.progress(successfulReferrals: Int): Float = when (status) {
    TierStatus.Claimed, TierStatus.Claimable -> 1f
    TierStatus.Locked -> (successfulReferrals.toFloat() / requiredReferrals).coerceIn(0f, 1f)
}

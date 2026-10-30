package vadiole.opaltest.data.referral

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReferralProgramDto(
    @SerialName("referral_code") val referralCode: String,
    @SerialName("share_url") val shareUrl: String,
    @SerialName("plan") val plan: String,
    @SerialName("successful_referrals") val successfulReferrals: Int,
    @SerialName("invitee_trial_days") val inviteeTrialDays: Int,
    @SerialName("tiers") val tiers: List<RewardTierDto>
)

@Serializable
data class RewardTierDto(
    @SerialName("id") val id: String,
    @SerialName("required_referrals") val requiredReferrals: Int,
    @SerialName("status") val status: String,
    @SerialName("reward") val reward: RewardDto
)

@Serializable
data class RewardDto(
    @SerialName("type") val type: String,
    @SerialName("stone") val stone: String? = null,
    @SerialName("term") val term: String? = null,
    @SerialName("fulfillment") val fulfillment: String? = null,
    @SerialName("value") val value: MoneyDto? = null
)

@Serializable
data class MoneyDto(@SerialName("amount_minor") val amountMinor: Long, @SerialName("currency") val currency: String)

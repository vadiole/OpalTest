package vadiole.opaltest.data.referral

interface ReferralApi {
    suspend fun getProgram(): ReferralProgramDto

    suspend fun claimReward(tierId: String): ReferralProgramDto
}

class ApiException(val httpCode: Int, val errorCode: String) : Exception("HTTP $httpCode: $errorCode")

object ApiErrorCodes {
    const val NOT_ELIGIBLE = "not_eligible"
    const val ALREADY_CLAIMED = "already_claimed"
    const val UNKNOWN_TIER = "unknown_tier"
}

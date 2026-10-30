package vadiole.opaltest.data.referral

import java.io.IOException
import kotlinx.coroutines.CancellationException
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram

interface ReferralRepository {
    suspend fun getProgram(): Outcome<ReferralProgram, ReferralError>

    suspend fun claimReward(tierId: String): Outcome<ReferralProgram, ReferralError>
}

class DefaultReferralRepository(private val api: ReferralApi) : ReferralRepository {
    override suspend fun getProgram() = request { api.getProgram() }

    override suspend fun claimReward(tierId: String) = request { api.claimReward(tierId) }

    private suspend fun request(block: suspend () -> ReferralProgramDto): Outcome<ReferralProgram, ReferralError> =
        try {
            block().toDomain()?.let { Outcome.Success(it) } ?: Outcome.Failure(ReferralError.Unknown)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Outcome.Failure(ReferralError.Network)
        } catch (e: ApiException) {
            Outcome.Failure(
                when (e.errorCode) {
                    ApiErrorCodes.NOT_ELIGIBLE -> ReferralError.NotEligible
                    ApiErrorCodes.ALREADY_CLAIMED -> ReferralError.AlreadyClaimed
                    else -> ReferralError.Unknown
                }
            )
        } catch (_: Exception) {
            Outcome.Failure(ReferralError.Unknown)
        }
}

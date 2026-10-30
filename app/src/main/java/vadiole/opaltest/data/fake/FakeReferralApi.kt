package vadiole.opaltest.data.fake

import java.io.IOException
import kotlinx.coroutines.delay
import vadiole.opaltest.data.referral.ReferralApi
import vadiole.opaltest.data.referral.ReferralProgramDto

class FakeReferralApi(private val backend: FakeReferralBackend) : ReferralApi {
    override suspend fun getProgram() = simulateNetwork(FakeCall.Load) { backend.program() }

    override suspend fun claimReward(tierId: String) = simulateNetwork(FakeCall.Claim) { backend.claim(tierId) }

    private suspend fun simulateNetwork(call: FakeCall, request: suspend () -> ReferralProgramDto): ReferralProgramDto {
        delay(backend.latency().millis)
        if (backend.consumeFailure(call)) throw IOException("Simulated network failure")
        return request()
    }
}

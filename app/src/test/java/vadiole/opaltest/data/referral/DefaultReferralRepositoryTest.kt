package vadiole.opaltest.data.referral

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vadiole.opaltest.data.fake.FakeReferralRules
import vadiole.opaltest.data.fake.FakeServerState
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.ReferralError

class DefaultReferralRepositoryTest {
    private class StubApi(private val failure: Exception?) : ReferralApi {
        override suspend fun getProgram() = failure?.let { throw it } ?: FakeReferralRules.program(FakeServerState())

        override suspend fun claimReward(tierId: String) = getProgram()
    }

    private suspend fun errorFor(failure: Exception): ReferralError {
        val outcome = DefaultReferralRepository(StubApi(failure)).claimReward("tier")
        assertTrue(outcome is Outcome.Failure)
        return (outcome as Outcome.Failure).error
    }

    @Test
    fun `success maps to domain`() = runTest {
        val outcome = DefaultReferralRepository(StubApi(null)).getProgram()

        assertEquals(FakeReferralRules.REFERRAL_CODE, (outcome as Outcome.Success).value.code)
    }

    @Test
    fun `failures map to typed errors`() = runTest {
        assertEquals(ReferralError.Network, errorFor(IOException()))
        assertEquals(ReferralError.NotEligible, errorFor(ApiException(403, ApiErrorCodes.NOT_ELIGIBLE)))
        assertEquals(ReferralError.AlreadyClaimed, errorFor(ApiException(409, ApiErrorCodes.ALREADY_CLAIMED)))
        assertEquals(ReferralError.Unknown, errorFor(ApiException(404, ApiErrorCodes.UNKNOWN_TIER)))
        assertEquals(ReferralError.Unknown, errorFor(IllegalStateException()))
    }

    @Test
    fun `cancellation is rethrown`() {
        assertThrows(CancellationException::class.java) {
            runTest { DefaultReferralRepository(StubApi(CancellationException())).getProgram() }
        }
    }
}

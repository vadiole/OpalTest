package vadiole.opaltest.data.fake

import androidx.datastore.core.DataStoreFactory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError

class FakeReferralApiTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "state.json") }

    private fun TestScope.backend(job: Job = Job(backgroundScope.coroutineContext[Job]), storage: File = file) =
        FakeReferralBackend(
            DataStoreFactory.create(
                serializer = FakeServerStateSerializer,
                scope = CoroutineScope(backgroundScope.coroutineContext + job),
                produceFile = { storage }
            )
        )

    private suspend fun FakeReferralBackend.debugState(): ReferralDebugState = (state.first() as Outcome.Success).value

    @Test
    fun `fail next load fails once then recovers`() = runTest {
        val backend = backend()
        val api = FakeReferralApi(backend)
        backend.setFailNextLoad(true)

        assertTrue(runCatching { api.getProgram() }.exceptionOrNull() is IOException)
        assertFalse(backend.debugState().failNextLoad)
        assertEquals(FakeReferralRules.REFERRAL_CODE, api.getProgram().referralCode)
    }

    @Test
    fun `claim persists across store instances`() = runTest {
        val job = Job(backgroundScope.coroutineContext[Job])
        val first = backend(job)
        first.setReferrals(5)
        FakeReferralApi(first).claimReward("pro_one_year")
        job.cancelAndJoin()

        val state = backend().debugState()

        assertEquals(Plan.Pro, state.plan)
        assertEquals(1, state.claimedTiers)
    }

    @Test
    fun `unreadable storage surfaces as a typed failure`() = runTest {
        val backend = backend(storage = folder.newFolder())

        assertEquals(Outcome.Failure(ReferralError.Network), backend.state.first())
        assertEquals(Outcome.Failure(ReferralError.Network), backend.setPlan(Plan.Pro))
    }
}

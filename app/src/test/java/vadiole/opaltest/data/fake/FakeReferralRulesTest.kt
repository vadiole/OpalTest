package vadiole.opaltest.data.fake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import vadiole.opaltest.data.referral.ApiErrorCodes
import vadiole.opaltest.data.referral.ApiException

class FakeReferralRulesTest {
    private fun statuses(state: FakeServerState) =
        FakeReferralRules.program(state).tiers.associate { it.id to it.status }

    @Test
    fun `tier status follows referral count and claims`() {
        val state = FakeServerState(successfulReferrals = 3, claimedTierIds = setOf("milestone_loyal"))

        val statuses = statuses(state)

        assertEquals("claimed", statuses["milestone_loyal"])
        assertEquals("claimable", statuses["milestone_soulful"])
        assertEquals("locked", statuses["pro_one_year"])
    }

    @Test
    fun `free year is hidden from pro users until claimed`() {
        assertTrue("pro_one_year" in statuses(FakeServerState(plan = ServerPlan.Free)))
        assertFalse("pro_one_year" in statuses(FakeServerState(plan = ServerPlan.Pro)))
        assertEquals(
            "claimed",
            statuses(FakeServerState(plan = ServerPlan.Pro, claimedTierIds = setOf("pro_one_year")))["pro_one_year"]
        )
    }

    @Test
    fun `claiming the free year upgrades to pro`() {
        val state = FakeReferralRules.claim(FakeServerState(successfulReferrals = 5), "pro_one_year")

        assertEquals(ServerPlan.Pro, state.plan)
        assertTrue("pro_one_year" in state.claimedTierIds)
    }

    @Test
    fun `claim rejects ineligible, duplicate and unknown tiers`() {
        fun errorOf(state: FakeServerState, tierId: String) =
            assertThrows(ApiException::class.java) { FakeReferralRules.claim(state, tierId) }.errorCode

        assertEquals(ApiErrorCodes.NOT_ELIGIBLE, errorOf(FakeServerState(successfulReferrals = 2), "milestone_soulful"))
        assertEquals(
            ApiErrorCodes.NOT_ELIGIBLE,
            errorOf(FakeServerState(plan = ServerPlan.Pro, successfulReferrals = 5), "pro_one_year")
        )
        assertEquals(
            ApiErrorCodes.ALREADY_CLAIMED,
            errorOf(
                FakeServerState(successfulReferrals = 1, claimedTierIds = setOf("milestone_loyal")),
                "milestone_loyal"
            )
        )
        assertEquals(ApiErrorCodes.UNKNOWN_TIER, errorOf(FakeServerState(successfulReferrals = 100), "nope"))
    }

    @Test
    fun `next tier skips hidden tiers and ends after the last one`() {
        assertEquals(5, FakeReferralRules.nextTierAt(FakeServerState(successfulReferrals = 3)))
        assertEquals(10, FakeReferralRules.nextTierAt(FakeServerState(plan = ServerPlan.Pro, successfulReferrals = 3)))
        assertNull(FakeReferralRules.nextTierAt(FakeServerState(successfulReferrals = 100)))
    }
}

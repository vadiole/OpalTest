package vadiole.opaltest.data.referral

import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Test
import vadiole.opaltest.data.fake.FakeReferralRules
import vadiole.opaltest.data.fake.FakeServerState
import vadiole.opaltest.domain.referral.Fulfillment
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.Money
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ProTerm
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.TierStatus

class ReferralMapperTest {
    private fun program(vararg tiers: RewardTierDto, plan: String = "free") = ReferralProgramDto(
        referralCode = "FR333",
        shareUrl = "https://example.com",
        plan = plan,
        successfulReferrals = 3,
        inviteeTrialDays = 30,
        tiers = tiers.toList()
    )

    @Test
    fun `maps every reward in the fake catalog`() {
        val program = FakeReferralRules.program(FakeServerState(successfulReferrals = 3)).toDomain()

        assertEquals(FakeReferralRules.catalog.size, program.tiers.size)
        assertEquals(
            listOf(
                Reward.Milestone(MilestoneStone.Loyal),
                Reward.Milestone(MilestoneStone.Soulful),
                Reward.ProAccess(ProTerm.OneYear, Fulfillment.Instant, null),
                Reward.Milestone(MilestoneStone.Popular),
                Reward.ProAccess(
                    ProTerm.TwoYears,
                    Fulfillment.ContactSupport,
                    Money(19900, Currency.getInstance("USD"))
                ),
                Reward.ProAccess(
                    ProTerm.Lifetime,
                    Fulfillment.ContactSupport,
                    Money(39900, Currency.getInstance("USD"))
                ),
                Reward.Mystery
            ),
            program.tiers.map { it.reward }
        )
        assertEquals(TierStatus.Claimable, program.tiers[1].status)
        assertEquals(TierStatus.Locked, program.tiers[2].status)
    }

    @Test
    fun `drops tiers the client cannot render and sorts by requirement`() {
        val program = program(
            RewardTierDto("b", 10, "locked", RewardDto(type = "mystery")),
            RewardTierDto("unknown_type", 5, "locked", RewardDto(type = "nft")),
            RewardTierDto("unknown_stone", 5, "locked", RewardDto(type = "milestone", stone = "ruby")),
            RewardTierDto("zero", 0, "locked", RewardDto(type = "mystery")),
            RewardTierDto("a", 1, "claimed", RewardDto(type = "milestone", stone = "loyal"))
        ).toDomain()

        assertEquals(listOf("a", "b"), program.tiers.map { it.id })
    }

    @Test
    fun `blank inviter maps to no inviter`() {
        assertEquals(null, program().copy(invitedBy = "  ").toDomain().invitedBy)
        assertEquals("Kenneth", program().copy(invitedBy = " Kenneth ").toDomain().invitedBy)
    }

    @Test
    fun `unknown plan and status fall back to the safest values`() {
        val program = program(
            RewardTierDto("a", 1, "pending_review", RewardDto(type = "mystery")),
            plan = "enterprise"
        ).toDomain()

        assertEquals(Plan.Free, program.plan)
        assertEquals(TierStatus.Locked, program.tiers.single().status)
    }

    @Test
    fun `unknown fulfillment needs support and duplicate tiers collapse`() {
        val program = program(
            RewardTierDto("a", 5, "locked", RewardDto(type = "pro_access", term = "one_year", fulfillment = "manual")),
            RewardTierDto("a", 5, "locked", RewardDto(type = "pro_access", term = "one_year"))
        ).toDomain()

        assertEquals(Fulfillment.ContactSupport, (program.tiers.single().reward as Reward.ProAccess).fulfillment)
    }

    @Test
    fun `invalid currency drops the value but keeps the tier`() {
        val codes = listOf("usd", "XYZ", "", "XXX", "EUR")
        val program = program(
            *codes.mapIndexed { index, code ->
                RewardTierDto(
                    id = code.ifEmpty { "empty" },
                    requiredReferrals = index + 1,
                    status = "locked",
                    reward = RewardDto(type = "pro_access", term = "lifetime", value = MoneyDto(39900, code))
                )
            }.toTypedArray()
        ).toDomain()

        assertEquals(
            listOf(null, null, null, null, Money(39900, Currency.getInstance("EUR"))),
            program.tiers.map { (it.reward as Reward.ProAccess).value }
        )
    }
}

package vadiole.opaltest.feature.referral

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import java.util.Currency
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.Money
import vadiole.opaltest.domain.referral.ProTerm
import vadiole.opaltest.domain.referral.Reward

@get:StringRes
internal val MilestoneStone.title: Int
    get() = when (this) {
        MilestoneStone.Loyal -> R.string.referral_milestone_loyal
        MilestoneStone.Soulful -> R.string.referral_milestone_soulful
        MilestoneStone.Popular -> R.string.referral_milestone_popular
    }

internal val Reward.isFreeYear: Boolean
    get() = this is Reward.ProAccess && term == ProTerm.OneYear

@Composable
internal fun Reward.title(): String = stringResource(
    when (this) {
        is Reward.Milestone -> stone.title

        is Reward.ProAccess -> when (term) {
            ProTerm.OneYear -> R.string.referral_pro_one_year
            ProTerm.TwoYears -> R.string.referral_pro_two_years
            ProTerm.Lifetime -> R.string.referral_pro_lifetime
        }

        Reward.Mystery -> R.string.referral_mystery
    }
)

@Composable
internal fun Reward.description(): String = when (this) {
    is Reward.Milestone -> stringResource(R.string.referral_milestone_description)

    is Reward.ProAccess -> {
        val text = stringResource(
            when (term) {
                ProTerm.OneYear -> R.string.referral_pro_one_year_description
                ProTerm.TwoYears -> R.string.referral_pro_two_years_description
                ProTerm.Lifetime -> R.string.referral_pro_lifetime_description
            }
        )
        value?.let { stringResource(R.string.referral_with_value, text, it.format()) } ?: text
    }

    Reward.Mystery -> stringResource(R.string.referral_mystery_description)
}

private fun Money.format(): String {
    val currency = Currency.getInstance(currencyCode)
    val amount = amountMinor.toBigDecimal().movePointLeft(currency.defaultFractionDigits)
    return NumberFormat.getCurrencyInstance().apply {
        this.currency = currency
        if (amount.stripTrailingZeros().scale() <= 0) maximumFractionDigits = 0
    }.format(amount)
}

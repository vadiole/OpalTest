package vadiole.opaltest.feature.referral

import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import vadiole.opaltest.domain.referral.Money

class RewardTextTest {
    private fun money(amountMinor: Long, code: String) = Money(amountMinor, Currency.getInstance(code))

    @Test
    fun `whole amounts drop the fraction`() {
        assertEquals("$199", money(19900, "USD").format(Locale.US))
    }

    @Test
    fun `fractional amounts keep the currency digits`() {
        assertEquals("$199.50", money(19950, "USD").format(Locale.US))
    }

    @Test
    fun `currency without minor units is not shifted`() {
        assertEquals("¥1,500", money(1500, "JPY").format(Locale.JAPAN).replace('￥', '¥'))
    }

    @Test
    fun `amount follows the locale`() {
        assertEquals("199,50 €", money(19950, "EUR").format(Locale.FRANCE).replace(' ', ' '))
    }

    @Test
    fun `counts get locale digit grouping`() {
        assertEquals("1,000,000", 1_000_000.formatCount(Locale.US))
        assertEquals("12", 12.formatCount(Locale.US))
    }
}

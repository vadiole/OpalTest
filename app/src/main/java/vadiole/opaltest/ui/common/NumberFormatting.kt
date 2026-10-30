package vadiole.opaltest.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Locale

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

fun Int.formatCount(locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(this)

@Composable
@ReadOnlyComposable
fun Int.formatCount(): String = formatCount(currentLocale())

fun Double.formatDecimal(maxFractionDigits: Int, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = maxFractionDigits }.format(this)

@Composable
@ReadOnlyComposable
fun Double.formatDecimal(maxFractionDigits: Int): String = formatDecimal(maxFractionDigits, currentLocale())

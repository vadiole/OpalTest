package vadiole.opaltest.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import vadiole.opaltest.R

private fun geist(weight: FontWeight) = Font(
    resId = R.font.geist,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Geist = FontFamily(
    geist(FontWeight.Normal),
    geist(FontWeight.Medium),
    geist(FontWeight.SemiBold),
    geist(FontWeight.Bold)
)

@Immutable
class OpalTypography(
    val caption: TextStyle,
    val callout: TextStyle,
    val calloutMedium: TextStyle,
    val body: TextStyle,
    val headline: TextStyle,
    val largeTitle: TextStyle
)

private val tight = (-0.015).em
private val loose = 0.0041.em
private val fullLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

val DefaultOpalTypography = OpalTypography(
    caption = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = tight
    ),
    callout = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = loose
    ),
    calloutMedium = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = loose
    ),
    body = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = tight
    ),
    headline = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    largeTitle = TextStyle(
        fontFamily = Geist,
        lineHeightStyle = fullLineHeight,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = tight
    )
)

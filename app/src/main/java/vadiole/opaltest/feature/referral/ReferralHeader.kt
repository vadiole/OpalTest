package vadiole.opaltest.feature.referral

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.ui.common.formatCount
import vadiole.opaltest.ui.common.opalRipple
import vadiole.opaltest.ui.common.pressFeedback
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun Summary(program: ReferralProgram) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.referral_gift_days,
                program.inviteeTrialDays,
                program.inviteeTrialDays
            ),
            style = OpalTheme.typography.headline.copy(lineBreak = LineBreak.Heading),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        val referrals = program.successfulReferrals
        val friends = pluralStringResource(R.plurals.referral_friends, referrals, referrals.formatCount())
        val template = stringResource(R.string.referral_referred, PLACEHOLDER)
        val accent = OpalTheme.colors.accent
        Text(
            text = if (referrals == 0) {
                AnnotatedString(stringResource(R.string.referral_referred_none))
            } else {
                buildAnnotatedString {
                    append(template.substringBefore(PLACEHOLDER))
                    withStyle(SpanStyle(brush = accent)) { append(friends) }
                    append(template.substringAfter(PLACEHOLDER))
                }
            },
            style = OpalTheme.typography.callout.copy(lineBreak = LineBreak.Heading),
            textAlign = TextAlign.Center
        )
    }
}

private const val PLACEHOLDER = "\u0000"

@Composable
internal fun Code(code: String, onCopy: () -> Unit) {
    val description = stringResource(R.string.referral_code_description, code.toList().joinToString(" "))
    val copyLabel = stringResource(R.string.referral_copy_invite)
    val cells = remember(code.length) { mutableStateListOf(*arrayOfNulls<Rect>(code.length)) }
    val shape = OpalTheme.shapes.small
    val fill = OpalTheme.colors.fill
    val interactionSource = remember { MutableInteractionSource() }
    val indicationSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused = interactionSource.collectIsFocusedAsState().value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.referral_code_title),
            style = OpalTheme.typography.headline,
            modifier = Modifier.clearAndSetSemantics {}
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints {
                val cellSize = ReferralDimens.codeCell
                val gap = ReferralDimens.codeCellGap
                val perRow = balancedCellsPerRow(code.length, fit = ((maxWidth + gap) / (cellSize.width + gap)).toInt())
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    maxItemsInEachRow = perRow,
                    modifier = Modifier
                        .pressFeedback(interactionSource, indicationSource)
                        .drawWithCache {
                            val mask = Path()
                            cells.forEach { cell ->
                                if (cell != null) {
                                    val outline = shape.createOutline(cell.size, layoutDirection, this)
                                    mask.addPath(Path().apply { addOutline(outline) }, cell.topLeft)
                                }
                            }
                            onDrawWithContent {
                                drawPath(mask, fill)
                                clipPath(mask) { this@onDrawWithContent.drawContent() }
                            }
                        }
                        .indication(
                            indicationSource,
                            opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill)
                        )
                        .clearAndSetSemantics {
                            contentDescription = description
                            heading()
                            this.focused = focused
                            onClick(label = copyLabel) {
                                onCopy()
                                true
                            }
                            requestFocus { focusRequester.requestFocus() }
                        }
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onLongClick = onCopy,
                            onClick = onCopy
                        )
                ) {
                    code.forEachIndexed { index, char ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(cellSize)
                                .onPlaced { cells[index] = it.boundsInParent() }
                        ) {
                            Text(text = char.toString(), style = OpalTheme.typography.largeTitle)
                        }
                    }
                }
            }
        }
    }
}

internal fun balancedCellsPerRow(length: Int, fit: Int): Int {
    if (length <= 0) return 1
    val rows = ceil(length / fit.coerceAtLeast(1).toFloat()).toInt()
    return ceil(length / rows.toFloat()).toInt()
}

@Preview(widthDp = 402, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CodeLengthsPreview() {
    OpalTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            modifier = Modifier
                .fillMaxWidth()
                .padding(OpalTheme.spacing.mediumLarge)
        ) {
            listOf("FR333", "ABCDEFG2", "ABCDEFGH23").forEach { Code(it, onCopy = {}) }
        }
    }
}

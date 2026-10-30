package vadiole.opaltest.feature.referral

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.opalRipple
import vadiole.opaltest.ui.common.outlineBorder
import vadiole.opaltest.ui.common.pressFeedback
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun InviteCard(invitedBy: String?, modifier: Modifier = Modifier) {
    val shape = OpalTheme.shapes.large
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(ReferralDimens.inviteCardHeight)
            .clip(shape)
            .outlineBorder(1.dp, OpalTheme.colors.borderSubtle, shape)
    ) {
        OpalImage(
            id = R.drawable.img_invite_card_background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
            modifier = Modifier.padding(horizontal = OpalTheme.spacing.medium)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opal_logo),
                contentDescription = stringResource(R.string.referral_opal_logo),
                tint = OpalTheme.colors.content
            )
            OpalImage(
                id = R.drawable.img_avatar_placeholder,
                contentDescription = null,
                modifier = Modifier.size(ReferralDimens.avatarSize)
            )
            if (invitedBy != null) {
                Text(
                    text = stringResource(R.string.referral_invited_by, invitedBy),
                    style = OpalTheme.typography.caption,
                    color = OpalTheme.colors.contentHigh,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
        val friends = pluralStringResource(R.plurals.referral_friends, referrals, referrals)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.referral_code_title),
            style = OpalTheme.typography.headline,
            modifier = Modifier.clearAndSetSemantics {}
        )
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
                    .indication(indicationSource, opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill))
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = copyLabel,
                        onLongClickLabel = copyLabel,
                        onLongClick = onCopy,
                        onClick = onCopy
                    )
                    .clearAndSetSemantics {
                        contentDescription = description
                        heading()
                    }
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

internal fun balancedCellsPerRow(length: Int, fit: Int): Int {
    if (length <= 0) return 1
    val rows = ceil(length / fit.coerceAtLeast(1).toFloat()).toInt()
    return ceil(length / rows.toFloat()).toInt()
}

@Preview(widthDp = 402, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InviteCardLongNamePreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            InviteCard("Kenneth", Modifier.fillMaxWidth())
            InviteCard("Maximilian Alexander von Hohenzollern-Sigmaringen", Modifier.fillMaxWidth())
        }
    }
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

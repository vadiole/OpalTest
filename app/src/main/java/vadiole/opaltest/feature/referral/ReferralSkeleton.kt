package vadiole.opaltest.feature.referral

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalSkeleton
import vadiole.opaltest.ui.common.OpalSkeletonBlock
import vadiole.opaltest.ui.common.OpalSkeletonLine
import vadiole.opaltest.ui.common.formatCount
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun Loading(headerState: ScrollState, listState: ScrollState) {
    OpalSkeleton(description = stringResource(R.string.referral_loading), modifier = Modifier.fillMaxSize()) {
        when (paneLayout()) {
            PaneLayout.Single -> SkeletonPane(Pane.Single, listState, Modifier.fillMaxWidth()) {
                InviteSkeleton()
                CodeSkeleton()
                ShareSkeleton()
                TiersSkeleton()
            }

            PaneLayout.TwoPane -> Row(Modifier.fillMaxSize()) {
                SkeletonPane(Pane.Start, headerState, Modifier.weight(1f)) {
                    InviteSkeleton()
                    CodeSkeleton()
                    ShareSkeleton()
                }
                SkeletonPane(Pane.End, listState, Modifier.weight(1f)) { TiersSkeleton() }
            }

            PaneLayout.ShortTwoPane -> Row(Modifier.fillMaxSize()) {
                SkeletonPane(
                    pane = Pane.Start,
                    scrollState = headerState,
                    modifier = Modifier.weight(1f),
                    spacing = OpalTheme.spacing.large,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodeSkeleton()
                    ShareSkeleton()
                }
                SkeletonPane(Pane.End, listState, Modifier.weight(1f)) {
                    CompactInviteSkeleton()
                    TiersSkeleton()
                }
            }
        }
    }
}

@Composable
private fun SkeletonPane(
    pane: Pane,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    spacing: Dp = OpalTheme.spacing.xLarge,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = paneAlignment(pane),
        verticalArrangement = Arrangement.spacedBy(spacing, verticalAlignment),
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState, enabled = scrollState.canScrollForward || scrollState.canScrollBackward)
            .padding(paneContentPadding(pane)),
        content = content
    )
}

@Composable
private fun InviteSkeleton() {
    OpalSkeletonBlock(paneItemModifier.height(ReferralDimens.inviteCardHeight), OpalTheme.shapes.large)
}

@Composable
private fun CompactInviteSkeleton() {
    OpalSkeletonBlock(paneItemModifier.height(ReferralDimens.compactInviteCardHeight), OpalTheme.shapes.xLarge)
}

@Composable
private fun CodeSkeleton() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
        modifier = paneItemModifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)
        ) {
            TextSkeleton(
                text = pluralStringResource(R.plurals.referral_gift_days, SKELETON_TRIAL_DAYS, SKELETON_TRIAL_DAYS),
                style = OpalTheme.typography.headline.copy(lineBreak = LineBreak.Heading),
                singleLineWidth = 220.dp
            )
            TextSkeleton(
                text = stringResource(
                    R.string.referral_referred,
                    pluralStringResource(
                        R.plurals.referral_friends,
                        SKELETON_REFERRALS,
                        SKELETON_REFERRALS.formatCount()
                    )
                ),
                style = OpalTheme.typography.callout.copy(lineBreak = LineBreak.Heading),
                singleLineWidth = 260.dp
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
        ) {
            OpalSkeletonLine(OpalTheme.typography.headline, 140.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(ReferralDimens.codeCellGap)) {
                repeat(SKELETON_CODE_LENGTH) { OpalSkeletonBlock(Modifier.size(ReferralDimens.codeCell)) }
            }
        }
    }
}

@Composable
private fun TextSkeleton(text: String, style: TextStyle, singleLineWidth: Dp) {
    BoxWithConstraints {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val lineWidths = remember(text, style, constraints.maxWidth, density) {
            val layout = textMeasurer.measure(text, style, constraints = Constraints(maxWidth = constraints.maxWidth))
            with(density) { List(layout.lineCount) { (layout.getLineRight(it) - layout.getLineLeft(it)).toDp() } }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (lineWidths.size <= 1) {
                OpalSkeletonLine(style, singleLineWidth)
            } else {
                lineWidths.forEach { OpalSkeletonLine(style, it) }
            }
        }
    }
}

@Composable
private fun ShareSkeleton() {
    OpalSkeletonBlock(paneItemModifier.height(OpalTheme.layout.controlHeight), OpalTheme.shapes.pill)
}

@Composable
private fun TiersSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
        modifier = paneItemModifier
    ) {
        repeat(SKELETON_TIERS) { index ->
            if (index > 0) Spacer(Modifier.height(ReferralDimens.tierConnectorHeight))
            TierCardSkeleton()
        }
    }
}

@Composable
private fun TierCardSkeleton() {
    OpalCard(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val artworkSize = tierArtworkSize(maxWidth)
            Row(
                horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OpalSkeletonBlock(Modifier.size(artworkSize), OpalTheme.shapes.medium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = OpalTheme.spacing.xxSmall)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                        OpalSkeletonLine(OpalTheme.typography.caption, 64.dp)
                        Column {
                            OpalSkeletonLine(OpalTheme.typography.body, 120.dp)
                            OpalSkeletonLine(OpalTheme.typography.caption, 160.dp)
                        }
                    }
                    OpalSkeletonBlock(
                        Modifier
                            .widthIn(max = ReferralDimens.progressMaxWidth)
                            .fillMaxWidth()
                            .height(OpalTheme.layout.progressBarHeight),
                        OpalTheme.shapes.pill
                    )
                }
            }
        }
    }
}

private const val SKELETON_CODE_LENGTH = 5
private const val SKELETON_TRIAL_DAYS = 30
private const val SKELETON_REFERRALS = 3
private const val SKELETON_TIERS = 3

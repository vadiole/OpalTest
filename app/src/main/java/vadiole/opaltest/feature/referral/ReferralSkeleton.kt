package vadiole.opaltest.feature.referral

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalSkeleton
import vadiole.opaltest.ui.common.SkeletonBlock
import vadiole.opaltest.ui.common.SkeletonLine
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun Loading(headerState: ScrollState, listState: ScrollState) {
    OpalSkeleton(description = stringResource(R.string.referral_loading), modifier = Modifier.fillMaxSize()) {
        if (isTwoPane()) {
            Row(Modifier.fillMaxSize()) {
                SkeletonPane(Pane.Start, headerState, Modifier.weight(1f)) { HeaderSkeleton() }
                SkeletonPane(Pane.End, listState, Modifier.weight(1f)) { TiersSkeleton() }
            }
        } else {
            SkeletonPane(Pane.Single, listState, Modifier.fillMaxWidth()) {
                HeaderSkeleton()
                TiersSkeleton()
            }
        }
    }
}

@Composable
private fun SkeletonPane(
    pane: Pane,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge),
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(paneContentPadding(pane)),
        content = content
    )
}

@Composable
private fun HeaderSkeleton() {
    SkeletonBlock(paneItemModifier.height(194.dp), OpalTheme.shapes.large)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)
        ) {
            SkeletonLine(OpalTheme.typography.headline, 220.dp)
            SkeletonLine(OpalTheme.typography.callout, 260.dp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
        ) {
            SkeletonLine(OpalTheme.typography.headline, 140.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(SKELETON_CODE_LENGTH) { SkeletonBlock(Modifier.size(width = 44.dp, height = 56.dp)) }
            }
        }
    }
    SkeletonBlock(paneItemModifier.height(50.dp), OpalTheme.shapes.pill)
}

@Composable
private fun TiersSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
        modifier = paneItemModifier
    ) {
        repeat(SKELETON_TIERS) { index ->
            if (index > 0) Spacer(Modifier.height(32.dp))
            TierCardSkeleton()
        }
    }
}

@Composable
private fun TierCardSkeleton() {
    OpalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBlock(Modifier.size(100.dp), OpalTheme.shapes.medium)
            Column(
                verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = OpalTheme.spacing.xxSmall)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                    SkeletonLine(OpalTheme.typography.caption, 64.dp)
                    Column {
                        SkeletonLine(OpalTheme.typography.body, 120.dp)
                        SkeletonLine(OpalTheme.typography.caption, 160.dp)
                    }
                }
                SkeletonBlock(
                    Modifier
                        .widthIn(max = 179.dp)
                        .fillMaxWidth()
                        .height(6.dp),
                    OpalTheme.shapes.pill
                )
            }
        }
    }
}

private const val SKELETON_CODE_LENGTH = 5
private const val SKELETON_TIERS = 3

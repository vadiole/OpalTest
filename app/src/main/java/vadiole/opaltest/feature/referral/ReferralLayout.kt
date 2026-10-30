package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.TierStatus
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.theme.OpalLayout
import vadiole.opaltest.ui.theme.OpalTheme

internal enum class Pane { Single, Start, End }

@Composable
internal fun isTwoPane(): Boolean = currentWindowAdaptiveInfo().windowSizeClass
    .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

@Composable
internal fun paneContentPadding(pane: Pane): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val spacing = OpalTheme.spacing
    val gap = spacing.xLarge / 2
    return PaddingValues(
        start = if (pane == Pane.End) gap else insets.calculateStartPadding(layoutDirection) + spacing.mediumLarge,
        end = if (pane == Pane.Start) gap else insets.calculateEndPadding(layoutDirection) + spacing.mediumLarge,
        top = insets.calculateTopPadding() + 72.dp,
        bottom = insets.calculateBottomPadding() + spacing.medium
    )
}

internal val paneItemModifier = Modifier
    .widthIn(max = OpalLayout.maxContentWidth)
    .fillMaxWidth()

@Composable
internal fun Program(
    program: ReferralProgram,
    claimingTierId: String?,
    reveal: ClaimRevealState,
    onIntent: (ReferralIntent) -> Unit,
    twoPane: Boolean,
    listState: LazyListState,
    headerState: LazyListState,
    tiersState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (twoPane) {
        Row(modifier.fillMaxSize()) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paneContentPadding(Pane.Start),
                state = headerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                programHeader(program, onIntent)
            }
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paneContentPadding(Pane.End),
                state = tiersState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                tiers(program, claimingTierId, reveal, onIntent, firstTopPadding = 0.dp)
            }
        }
    } else {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = paneContentPadding(Pane.Single),
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            programHeader(program, onIntent)
            tiers(program, claimingTierId, reveal, onIntent, firstTopPadding = OpalTheme.spacing.xLarge)
        }
    }
}

private enum class HeaderItem { Invite, Code, Share }

internal fun LazyListScope.programHeader(program: ReferralProgram, onIntent: (ReferralIntent) -> Unit) {
    item(key = HeaderItem.Invite) {
        InviteCard(program.invitedBy, paneItemModifier)
    }
    item(key = HeaderItem.Code) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            modifier = paneItemModifier.padding(top = OpalTheme.spacing.xLarge)
        ) {
            Summary(program)
            Code(program.code, onCopy = { onIntent(ReferralIntent.CopyInvite) })
        }
    }
    item(key = HeaderItem.Share) {
        OpalButton(
            text = stringResource(R.string.referral_share),
            onClick = { onIntent(ReferralIntent.Share) },
            leadingIcon = R.drawable.ic_share,
            modifier = paneItemModifier.padding(top = OpalTheme.spacing.xLarge)
        )
    }
}

internal fun LazyListScope.tiers(
    program: ReferralProgram,
    claimingTierId: String?,
    reveal: ClaimRevealState,
    onIntent: (ReferralIntent) -> Unit,
    firstTopPadding: Dp
) {
    val activeTierId = program.tiers.firstOrNull { it.status != TierStatus.Claimed }?.id
    itemsIndexed(program.tiers, key = { _, tier -> tier.id }) { index, tier ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
            modifier = paneItemModifier.padding(top = if (index == 0) firstTopPadding else OpalTheme.spacing.xxSmall)
        ) {
            if (index > 0) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_south),
                        contentDescription = null,
                        tint = OpalTheme.colors.contentTertiary
                    )
                }
            }
            TierCard(
                tier = tier,
                successfulReferrals = program.successfulReferrals,
                claiming = tier.id == claimingTierId,
                claimable = claimingTierId == null || tier.id == claimingTierId,
                active = tier.id == activeTierId,
                reveal = reveal,
                onClaim = { onIntent(ReferralIntent.Claim(tier.id)) }
            )
        }
    }
}

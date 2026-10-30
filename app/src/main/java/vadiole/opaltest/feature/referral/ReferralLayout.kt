package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.TierStatus
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.theme.OpalLayout
import vadiole.opaltest.ui.theme.OpalTheme

internal enum class PaneLayout { Single, TwoPane, ShortTwoPane }

internal enum class Pane { Single, Start, End }

@Composable
internal fun paneLayout(): PaneLayout {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> PaneLayout.Single
        !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> PaneLayout.ShortTwoPane
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> PaneLayout.TwoPane
        else -> PaneLayout.Single
    }
}

@Composable
internal fun paneContentPadding(pane: Pane): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val spacing = OpalTheme.spacing
    val gap = spacing.xLarge / 2
    return PaddingValues(
        start = if (pane == Pane.End) gap else insets.calculateStartPadding(layoutDirection) + spacing.mediumLarge,
        end = if (pane == Pane.Start) gap else insets.calculateEndPadding(layoutDirection) + spacing.mediumLarge,
        top = insets.calculateTopPadding() + ReferralDimens.contentTop,
        bottom = insets.calculateBottomPadding() + spacing.medium
    )
}

internal fun paneAlignment(pane: Pane): Alignment.Horizontal = when (pane) {
    Pane.Single -> Alignment.CenterHorizontally
    Pane.Start -> Alignment.End
    Pane.End -> Alignment.Start
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
    layout: PaneLayout,
    listState: LazyListState,
    headerState: LazyListState,
    tiersState: LazyListState,
    modifier: Modifier = Modifier
) {
    val gap = OpalTheme.spacing.xLarge
    when (layout) {
        PaneLayout.Single -> ProgramPane(Pane.Single, listState, modifier.fillMaxSize()) {
            invite(program, gapAfter = gap)
            codeAndShare(program, onIntent, gapAfter = gap)
            tiers(program, claimingTierId, reveal, onIntent)
        }

        PaneLayout.TwoPane -> Row(modifier.fillMaxSize()) {
            ProgramPane(Pane.Start, headerState) {
                invite(program, gapAfter = gap)
                codeAndShare(program, onIntent, gapAfter = 0.dp)
            }
            ProgramPane(Pane.End, tiersState) {
                tiers(program, claimingTierId, reveal, onIntent)
            }
        }

        PaneLayout.ShortTwoPane -> Row(modifier.fillMaxSize()) {
            ProgramPane(Pane.Start, headerState, verticalArrangement = Arrangement.Center) {
                codeAndShare(program, onIntent, gapAfter = 0.dp)
            }
            ProgramPane(Pane.End, tiersState) {
                invite(program, gapAfter = gap)
                tiers(program, claimingTierId, reveal, onIntent)
            }
        }
    }
}

@Composable
private fun RowScope.ProgramPane(
    pane: Pane,
    state: LazyListState,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) = ProgramPane(
    pane = pane,
    state = state,
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    verticalArrangement = verticalArrangement,
    content = content
)

@Composable
private fun ProgramPane(
    pane: Pane,
    state: LazyListState,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        horizontalAlignment = paneAlignment(pane),
        verticalArrangement = verticalArrangement,
        contentPadding = paneContentPadding(pane),
        state = state,
        modifier = modifier,
        content = content
    )
}

private enum class HeaderItem { Invite, Code, Share }

private fun LazyListScope.invite(program: ReferralProgram, gapAfter: Dp) {
    item(key = HeaderItem.Invite) {
        InviteCard(program.invitedBy, paneItemModifier.padding(bottom = gapAfter))
    }
}

private fun LazyListScope.codeAndShare(program: ReferralProgram, onIntent: (ReferralIntent) -> Unit, gapAfter: Dp) {
    item(key = HeaderItem.Code) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            modifier = paneItemModifier
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
            modifier = paneItemModifier.padding(top = OpalTheme.spacing.xLarge, bottom = gapAfter)
        )
    }
}

private fun LazyListScope.tiers(
    program: ReferralProgram,
    claimingTierId: String?,
    reveal: ClaimRevealState,
    onIntent: (ReferralIntent) -> Unit
) {
    val activeTierId = program.tiers.firstOrNull { it.status != TierStatus.Claimed }?.id
    itemsIndexed(program.tiers, key = { _, tier -> tier.id }) { index, tier ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
            modifier = paneItemModifier.padding(top = if (index == 0) 0.dp else OpalTheme.spacing.xxSmall)
        ) {
            if (index > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(ReferralDimens.tierConnectorHeight)
                ) {
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
                claimAction = claimAction(tier.id, claimingTierId),
                active = tier.id == activeTierId,
                reveal = reveal,
                onClaim = {
                    (tier.reward as? Reward.Milestone)?.let { reveal.prefetch(it.stone.revealImage) }
                    onIntent(ReferralIntent.Claim(tier.id))
                }
            )
        }
    }
}

internal data class ScrollPosition(val index: Int = 0, val offset: Int = 0)

internal data class PanePositions(
    val single: ScrollPosition = ScrollPosition(),
    val start: ScrollPosition = ScrollPosition(),
    val end: ScrollPosition = ScrollPosition()
)

private val HEADER_ITEMS = HeaderItem.entries.size

private fun PanePositions.singleListPosition(layout: PaneLayout): ScrollPosition = when (layout) {
    PaneLayout.Single -> single

    PaneLayout.TwoPane -> if (end != ScrollPosition()) {
        ScrollPosition(HEADER_ITEMS + end.index, end.offset)
    } else {
        start
    }

    PaneLayout.ShortTwoPane -> when {
        end.index > 0 -> ScrollPosition(HEADER_ITEMS - 1 + end.index, end.offset)
        start != ScrollPosition() -> ScrollPosition(start.index + 1, start.offset)
        else -> end
    }
}

private fun ScrollPosition.toPanes(layout: PaneLayout): PanePositions = when (layout) {
    PaneLayout.Single -> PanePositions(single = this)

    PaneLayout.TwoPane -> if (index >= HEADER_ITEMS) {
        PanePositions(end = ScrollPosition(index - HEADER_ITEMS, offset))
    } else {
        PanePositions(start = this)
    }

    PaneLayout.ShortTwoPane -> when {
        index == 0 -> PanePositions(end = this)
        index < HEADER_ITEMS -> PanePositions(start = ScrollPosition(index - 1, offset))
        else -> PanePositions(end = ScrollPosition(index - HEADER_ITEMS + 1, offset))
    }
}

internal fun remapScroll(from: PaneLayout, to: PaneLayout, positions: PanePositions): PanePositions =
    positions.singleListPosition(from).toPanes(to)

@Composable
internal fun KeepScrollAcrossLayouts(
    layout: PaneLayout,
    listState: LazyListState,
    headerState: LazyListState,
    tiersState: LazyListState
) {
    var shownLayout by rememberSaveable { mutableStateOf(layout) }
    SideEffect {
        if (shownLayout != layout) {
            val target = remapScroll(
                from = shownLayout,
                to = layout,
                positions = PanePositions(listState.position, headerState.position, tiersState.position)
            )
            if (layout == PaneLayout.Single) {
                listState.requestScrollToItem(target.single.index, target.single.offset)
            } else {
                headerState.requestScrollToItem(target.start.index, target.start.offset)
                tiersState.requestScrollToItem(target.end.index, target.end.offset)
            }
            shownLayout = layout
        }
    }
}

private val LazyListState.position: ScrollPosition
    get() = ScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

@Preview(widthDp = 732, heightDp = 412)
@Composable
private fun ReferralShortTwoPanePreview() {
    OpalTheme {
        ReferralContent(state = ReferralState.Ready(previewProgram()), onIntent = {}, onBack = {})
    }
}

@Preview(widthDp = 732, heightDp = 412)
@Composable
private fun ReferralShortTwoPaneLoadingPreview() {
    OpalTheme {
        ReferralContent(state = ReferralState.Loading, onIntent = {}, onBack = {})
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ReferralTwoPaneLoadingPreview() {
    OpalTheme {
        ReferralContent(state = ReferralState.Loading, onIntent = {}, onBack = {})
    }
}

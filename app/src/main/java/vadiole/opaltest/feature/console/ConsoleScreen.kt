package vadiole.opaltest.feature.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.data.fake.ReferralDebugState
import vadiole.opaltest.data.fake.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalChip
import vadiole.opaltest.ui.common.OpalSkeleton
import vadiole.opaltest.ui.common.OpalToggleChip
import vadiole.opaltest.ui.common.SkeletonBlock
import vadiole.opaltest.ui.common.SkeletonLine
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ConsoleScreen(
    onOpenReferrals: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsoleViewModel = viewModel { ConsoleViewModel(container.referralDebugControls) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val openReferrals by rememberUpdatedState(onOpenReferrals)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConsoleEffect.FriendAdded -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                ConsoleEffect.TierReached -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                ConsoleEffect.OpenReferrals -> openReferrals()
            }
        }
    }
    ConsoleContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun ConsoleContent(state: ConsoleState, onIntent: (ConsoleIntent) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
    ) {
        val minHeight = maxHeight
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .safeDrawingPadding()
                .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.large)
                .wrapContentWidth()
                .widthIn(max = OpalTheme.layout.maxContentWidth)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
                Text(stringResource(R.string.console_title), style = OpalTheme.typography.largeTitle)
                val debug = state.debug
                when {
                    state.loadFailed -> LoadError(onRetry = { onIntent(ConsoleIntent.Retry) })

                    debug != null -> {
                        Status(debug)
                        Controls(debug, state.isConfirmingReset, onIntent)
                    }

                    else -> Loading()
                }
            }
            Actions(
                enabled = state.debug != null,
                nextTierEnabled = state.debug?.nextTierAt != null,
                onIntent = onIntent,
                modifier = Modifier.padding(top = OpalTheme.spacing.xLarge)
            )
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    OpalSkeleton(description = stringResource(R.string.console_loading), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
            Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall)) {
                SkeletonLine(OpalTheme.typography.body, 180.dp)
                SkeletonLine(OpalTheme.typography.callout, 150.dp)
                SkeletonLine(OpalTheme.typography.callout, 120.dp)
            }
            OpalCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                    ControlRowSkeleton(chipWidths = listOf(56.dp, 52.dp))
                    ControlRowSkeleton(chipWidths = listOf(64.dp, 80.dp, 56.dp))
                    ControlRowSkeleton(chipWidths = listOf(96.dp))
                    SkeletonBlock(Modifier.size(width = 88.dp, height = 40.dp), OpalTheme.shapes.pill)
                }
            }
        }
    }
}

@Composable
private fun ControlRowSkeleton(chipWidths: List<Dp>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonLine(OpalTheme.typography.caption, 32.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
            chipWidths.forEach { SkeletonBlock(Modifier.size(width = it, height = 40.dp), OpalTheme.shapes.pill) }
        }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    OpalCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(stringResource(R.string.console_load_error), style = OpalTheme.typography.body)
            OpalButton(
                text = stringResource(R.string.console_retry),
                onClick = onRetry,
                style = OpalButtonStyle.Primary,
                size = OpalButtonSize.Small
            )
        }
    }
}

@Composable
private fun Status(debug: ReferralDebugState, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall), modifier = modifier) {
        val plan = stringResource(if (debug.plan == Plan.Pro) R.string.console_plan_pro else R.string.console_plan_free)
        val friends = pluralStringResource(
            R.plurals.console_friends,
            debug.successfulReferrals,
            debug.successfulReferrals
        )
        Text(stringResource(R.string.console_status, plan, friends), style = OpalTheme.typography.body)
        val next = debug.nextTierAt
        Text(
            text = if (next != null) {
                pluralStringResource(R.plurals.console_next_reward, next, next)
            } else {
                stringResource(R.string.console_all_rewards_reached)
            },
            style = OpalTheme.typography.callout,
            color = OpalTheme.colors.contentSecondary
        )
        Text(
            text = pluralStringResource(R.plurals.console_claimed, debug.claimedTiers, debug.claimedTiers),
            style = OpalTheme.typography.callout,
            color = OpalTheme.colors.contentSecondary
        )
    }
}

@Composable
private fun Controls(
    debug: ReferralDebugState,
    isConfirmingReset: Boolean,
    onIntent: (ConsoleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf(
        stringResource(R.string.console_label_plan),
        stringResource(R.string.console_label_delay),
        stringResource(R.string.console_label_fail)
    )
    val labelStyle = OpalTheme.typography.caption
    val textMeasurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) {
        labels.maxOf { textMeasurer.measure(it, labelStyle, softWrap = false).size.width }.toDp()
    }
    OpalCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
            ControlRow(labels[0], labelWidth, Modifier.selectableGroup()) {
                OpalChip(
                    text = stringResource(R.string.console_free),
                    selected = debug.plan == Plan.Free,
                    onClick = { onIntent(ConsoleIntent.SetPlan(Plan.Free)) }
                )
                OpalChip(
                    text = stringResource(R.string.console_pro),
                    selected = debug.plan == Plan.Pro,
                    onClick = { onIntent(ConsoleIntent.SetPlan(Plan.Pro)) }
                )
            }
            ControlRow(labels[1], labelWidth, Modifier.selectableGroup()) {
                SimulatedLatency.entries.forEach { latency ->
                    OpalChip(
                        text = stringResource(latency.label()),
                        selected = debug.latency == latency,
                        onClick = { onIntent(ConsoleIntent.SetLatency(latency)) }
                    )
                }
            }
            ControlRow(labels[2], labelWidth) {
                OpalToggleChip(
                    text = stringResource(R.string.console_fail_next_load),
                    checked = debug.failNextLoad,
                    onCheckedChange = { onIntent(ConsoleIntent.ToggleFailNextLoad) }
                )
            }
            ResetButton(isConfirming = isConfirmingReset, onClick = { onIntent(ConsoleIntent.Reset) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlRow(label: String, labelWidth: Dp, modifier: Modifier = Modifier, chips: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentSecondary,
            softWrap = false,
            modifier = Modifier.width(labelWidth)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) { chips() }
    }
}

@Composable
private fun ResetButton(isConfirming: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val confirmDescription = stringResource(R.string.console_reset_confirm_state)
    OpalButton(
        text = stringResource(if (isConfirming) R.string.console_reset_confirm else R.string.console_reset),
        onClick = onClick,
        style = if (isConfirming) OpalButtonStyle.Accent else OpalButtonStyle.Primary,
        size = OpalButtonSize.Small,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (isConfirming) stateDescription = confirmDescription
        }
    )
}

private fun SimulatedLatency.label(): Int = when (this) {
    SimulatedLatency.None -> R.string.console_delay_none
    SimulatedLatency.Normal -> R.string.console_delay_normal
    SimulatedLatency.Slow -> R.string.console_delay_slow
}

@Composable
private fun Actions(
    enabled: Boolean,
    nextTierEnabled: Boolean,
    onIntent: (ConsoleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
            OpalButton(
                text = stringResource(R.string.console_add_friend),
                onClick = { onIntent(ConsoleIntent.AddFriend) },
                style = OpalButtonStyle.Primary,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            OpalButton(
                text = stringResource(R.string.console_next_tier),
                onClick = { onIntent(ConsoleIntent.AdvanceToNextTier) },
                style = OpalButtonStyle.Primary,
                enabled = enabled && nextTierEnabled,
                modifier = Modifier.weight(1f)
            )
        }
        OpalButton(
            text = stringResource(R.string.console_open_referrals),
            onClick = { onIntent(ConsoleIntent.OpenReferrals) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val previewDebug = ReferralDebugState(
    plan = Plan.Free,
    successfulReferrals = 3,
    claimedTiers = 1,
    nextTierAt = 5,
    latency = SimulatedLatency.Normal,
    failNextLoad = false,
    failNextClaim = false
)

@Preview(name = "Phone", widthDp = 402, heightDp = 874)
@Composable
private fun ConsolePhonePreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(previewDebug, isLoading = false), onIntent = {}) }
}

@Preview(name = "Landscape", widthDp = 874, heightDp = 402)
@Composable
private fun ConsoleLandscapePreview() {
    OpalTheme {
        ConsoleContent(state = ConsoleState(previewDebug.copy(plan = Plan.Pro), isLoading = false), onIntent = {})
    }
}

@Preview(name = "Tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun ConsoleTabletPreview() {
    OpalTheme {
        ConsoleContent(
            state = ConsoleState(
                previewDebug.copy(successfulReferrals = 100, nextTierAt = null),
                isLoading = false
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Loading", widthDp = 402, heightDp = 874)
@Composable
private fun ConsoleLoadingPreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(), onIntent = {}) }
}

@Preview(name = "Loading landscape", widthDp = 874, heightDp = 402)
@Composable
private fun ConsoleLoadingLandscapePreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(), onIntent = {}) }
}

@Preview(name = "Load error", widthDp = 402, heightDp = 874)
@Composable
private fun ConsoleLoadErrorPreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(isLoading = false, loadFailed = true), onIntent = {}) }
}

@Preview(name = "Confirm reset", widthDp = 402, heightDp = 874)
@Composable
private fun ConsoleConfirmResetPreview() {
    OpalTheme {
        ConsoleContent(state = ConsoleState(previewDebug, isLoading = false, isConfirmingReset = true), onIntent = {})
    }
}

@Preview(name = "Large font", widthDp = 402, heightDp = 874, fontScale = 2f)
@Composable
private fun ConsoleLargeFontPreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(previewDebug, isLoading = false), onIntent = {}) }
}

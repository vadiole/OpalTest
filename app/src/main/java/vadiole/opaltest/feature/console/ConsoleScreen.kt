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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.data.fake.ReferralDebugState
import vadiole.opaltest.data.fake.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalChip
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
                .widthIn(max = 480.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
                Text(stringResource(R.string.console_title), style = OpalTheme.typography.largeTitle)
                state.debug?.let {
                    Status(it)
                    Controls(it, onIntent)
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
private fun Controls(debug: ReferralDebugState, onIntent: (ConsoleIntent) -> Unit, modifier: Modifier = Modifier) {
    OpalCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
            ControlRow(stringResource(R.string.console_label_plan)) {
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
            ControlRow(stringResource(R.string.console_label_delay)) {
                SimulatedLatency.entries.forEach { latency ->
                    OpalChip(
                        text = stringResource(latency.label()),
                        selected = debug.latency == latency,
                        onClick = { onIntent(ConsoleIntent.SetLatency(latency)) }
                    )
                }
            }
            ControlRow(stringResource(R.string.console_label_fail)) {
                OpalChip(
                    text = stringResource(R.string.console_fail_next_load),
                    selected = debug.failNextLoad,
                    onClick = { onIntent(ConsoleIntent.ToggleFailNextLoad) }
                )
            }
            OpalChip(
                text = stringResource(R.string.console_reset),
                selected = false,
                onClick = { onIntent(ConsoleIntent.Reset) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlRow(label: String, modifier: Modifier = Modifier, chips: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = label,
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentSecondary,
            modifier = Modifier.width(56.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) { chips() }
    }
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
    OpalTheme { ConsoleContent(state = ConsoleState(previewDebug), onIntent = {}) }
}

@Preview(name = "Landscape", widthDp = 874, heightDp = 402)
@Composable
private fun ConsoleLandscapePreview() {
    OpalTheme { ConsoleContent(state = ConsoleState(previewDebug.copy(plan = Plan.Pro)), onIntent = {}) }
}

@Preview(name = "Tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun ConsoleTabletPreview() {
    OpalTheme {
        ConsoleContent(
            state = ConsoleState(
                previewDebug.copy(successfulReferrals = 100, nextTierAt = null)
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

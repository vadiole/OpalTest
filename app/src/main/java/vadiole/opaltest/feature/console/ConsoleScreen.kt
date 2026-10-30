package vadiole.opaltest.feature.console

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.rememberPacedPhase
import vadiole.opaltest.ui.theme.OpalMotion
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        viewModel.effects.flowWithLifecycle(lifecycle).collect { effect ->
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
    val title = stringResource(R.string.console_title)
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val wide = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val short = !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val scrollState = rememberScrollState()
    val shown = rememberShownLoad(state.load)
    val spacing = OpalTheme.spacing
    val maxContentWidth = OpalTheme.layout.maxContentWidth
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
            .semantics { paneTitle = title }
            .safeDrawingPadding()
    ) {
        if (wide && short) {
            val buttonMaxWidth = (maxWidth - spacing.mediumLarge * 2 - spacing.xLarge) / 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xLarge, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.mediumLarge)
            ) {
                Body(
                    title = title,
                    shown = shown,
                    onIntent = onIntent,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = maxContentWidth)
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(vertical = spacing.large)
                )
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .widthIn(max = buttonMaxWidth)
                        .width(IntrinsicSize.Max)
                        .fillMaxHeight()
                        .padding(vertical = spacing.large)
                ) {
                    OpenReferralsButton(onIntent, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                verticalArrangement = if (wide) Arrangement.Center else Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Body(
                    title = title,
                    shown = shown,
                    onIntent = onIntent,
                    modifier = Modifier
                        .weight(1f, fill = !wide)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = spacing.mediumLarge, vertical = spacing.large)
                        .wrapContentWidth()
                        .widthIn(max = maxContentWidth)
                )
                OpenReferralsButton(
                    onIntent = onIntent,
                    modifier = Modifier
                        .padding(
                            start = spacing.mediumLarge,
                            top = spacing.small,
                            end = spacing.mediumLarge,
                            bottom = spacing.large
                        )
                        .widthIn(max = maxContentWidth)
                        .fillMaxWidth()
                )
            }
        }
    }
}

private enum class Shown { Loading, Failed, Ready }

private val ConsoleLoad.shown: Shown
    get() = when (this) {
        ConsoleLoad.Loading -> Shown.Loading
        ConsoleLoad.Failed -> Shown.Failed
        is ConsoleLoad.Ready -> Shown.Ready
    }

@Composable
private fun rememberShownLoad(load: ConsoleLoad): ConsoleLoad? {
    val target = load.shown
    return when (rememberPacedPhase(target, loading = Shown.Loading)) {
        target, Shown.Ready -> load
        null -> null
        Shown.Loading -> ConsoleLoad.Loading
        Shown.Failed -> ConsoleLoad.Failed
    }
}

@Composable
private fun Body(title: String, shown: ConsoleLoad?, onIntent: (ConsoleIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large), modifier = modifier) {
        Text(
            text = title,
            style = OpalTheme.typography.largeTitle,
            modifier = Modifier.semantics { heading() }
        )
        AnimatedContent(
            targetState = shown,
            contentKey = { it?.shown },
            transitionSpec = {
                if (initialState == null) {
                    EnterTransition.None togetherWith ExitTransition.None using null
                } else {
                    fadeIn(OpalMotion.CrossFade) togetherWith fadeOut(OpalMotion.CrossFade) using null
                }
            },
            label = "consoleBody"
        ) { load ->
            when (load) {
                null -> Box(Modifier.fillMaxWidth())

                ConsoleLoad.Loading -> Loading()

                ConsoleLoad.Failed -> LoadError(onRetry = { onIntent(ConsoleIntent.Retry) })

                is ConsoleLoad.Ready -> Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
                    Status(load.debug)
                    Controls(load.debug, load.isConfirmingReset, onIntent)
                }
            }
        }
    }
}

@Composable
private fun OpenReferralsButton(onIntent: (ConsoleIntent) -> Unit, modifier: Modifier = Modifier) {
    OpalButton(
        text = stringResource(R.string.console_open_referrals),
        onClick = { onIntent(ConsoleIntent.OpenReferrals) },
        modifier = modifier
    )
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
                style = OpalButtonStyle.Neutral,
                size = OpalButtonSize.Small
            )
        }
    }
}

@Composable
private fun Status(debug: ReferralDebugState, modifier: Modifier = Modifier) {
    val format = rememberNumberFormat()
    val claimed = debug.claimedTiers
    Text(
        text = if (claimed == 0) {
            stringResource(R.string.console_no_rewards_claimed)
        } else {
            pluralStringResource(R.plurals.console_claimed, claimed, format.format(claimed))
        },
        style = OpalTheme.typography.body,
        modifier = modifier
    )
}

private val previewDebug = ReferralDebugState(
    plan = Plan.Free,
    successfulReferrals = 3,
    maxReferrals = 999,
    claimedTiers = 1,
    nextTierAt = 5,
    latency = SimulatedLatency.Normal,
    failNextLoad = false,
    failNextClaim = false
)

private val previewReady = ConsoleState(ConsoleLoad.Ready(previewDebug))

@Preview(name = "Phone", widthDp = 402, heightDp = 874)
@Composable
private fun ConsolePhonePreview() {
    OpalTheme { ConsoleContent(state = previewReady, onIntent = {}) }
}

@Preview(name = "Landscape", widthDp = 874, heightDp = 402)
@Composable
private fun ConsoleLandscapePreview() {
    OpalTheme {
        ConsoleContent(state = ConsoleState(ConsoleLoad.Ready(previewDebug.copy(plan = Plan.Pro))), onIntent = {})
    }
}

@Preview(name = "Tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun ConsoleTabletPreview() {
    OpalTheme { ConsoleContent(state = previewReady, onIntent = {}) }
}

@Preview(name = "All rewards unlocked", widthDp = 402, heightDp = 874)
@Composable
private fun ConsoleAllUnlockedPreview() {
    OpalTheme {
        ConsoleContent(
            state = ConsoleState(
                ConsoleLoad.Ready(previewDebug.copy(successfulReferrals = 999, claimedTiers = 7, nextTierAt = null))
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
    OpalTheme { ConsoleContent(state = ConsoleState(ConsoleLoad.Failed), onIntent = {}) }
}

@Preview(name = "Confirm reset", widthDp = 402, heightDp = 874)
@Composable
private fun ConsoleConfirmResetPreview() {
    OpalTheme {
        ConsoleContent(state = ConsoleState(ConsoleLoad.Ready(previewDebug, isConfirmingReset = true)), onIntent = {})
    }
}

@Preview(name = "Large font", widthDp = 402, heightDp = 874, fontScale = 2f)
@Composable
private fun ConsoleLargeFontPreview() {
    OpalTheme { ConsoleContent(state = previewReady, onIntent = {}) }
}

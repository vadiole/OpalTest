package vadiole.opaltest.feature.console

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import java.text.NumberFormat
import kotlin.time.Duration.Companion.milliseconds
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
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
private fun Loading(modifier: Modifier = Modifier) {
    val labelWidth = rememberLabelWidth()
    OpalSkeleton(description = stringResource(R.string.console_loading), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
            SkeletonLine(OpalTheme.typography.body, 160.dp)
            OpalCard(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints {
                    val stacked = stacksLabels(labelWidth, maxWidth)
                    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                        ControlRowSkeleton(stacked, chipWidths = listOf(56.dp, 52.dp))
                        ControlRowSkeleton(stacked, chipWidths = listOf(44.dp, 128.dp), valueWidth = 12.dp)
                        ControlRowSkeleton(stacked, chipWidths = listOf(64.dp, 60.dp, 48.dp))
                        ControlRowSkeleton(stacked, chipWidths = listOf(96.dp, 100.dp))
                        SkeletonBlock(
                            Modifier.size(width = 104.dp, height = OpalTheme.layout.controlHeightSmall),
                            OpalTheme.shapes.pill
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlRowSkeleton(stacked: Boolean, chipWidths: List<Dp>, valueWidth: Dp? = null) {
    LabeledRow(
        stacked = stacked,
        label = { SkeletonLine(OpalTheme.typography.caption, 40.dp) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (valueWidth != null) {
                SkeletonLine(
                    OpalTheme.typography.body,
                    valueWidth,
                    Modifier.padding(horizontal = OpalTheme.spacing.xxSmall)
                )
            }
            chipWidths.forEach {
                SkeletonBlock(
                    Modifier.size(width = it, height = OpalTheme.layout.controlHeightSmall),
                    OpalTheme.shapes.pill
                )
            }
        }
    }
}

@Composable
private fun LabeledRow(stacked: Boolean, label: @Composable () -> Unit, content: @Composable () -> Unit) {
    val gap = if (stacked) OpalTheme.spacing.xxSmall else OpalTheme.spacing.small
    Layout(contents = listOf(label, content)) { (labelMeasurables, contentMeasurables), constraints ->
        val gapPx = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaceables = labelMeasurables.map { it.measure(loose) }
        val labelWidth = labelPlaceables.maxOfOrNull { it.width } ?: 0
        val labelHeight = labelPlaceables.maxOfOrNull { it.height } ?: 0
        val contentConstraints = if (stacked) {
            loose
        } else {
            loose.copy(maxWidth = (loose.maxWidth - labelWidth - gapPx).coerceAtLeast(0))
        }
        val contentPlaceables = contentMeasurables.map { it.measure(contentConstraints) }
        val contentWidth = contentPlaceables.maxOfOrNull { it.width } ?: 0
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0
        val width = if (stacked) maxOf(labelWidth, contentWidth) else labelWidth + gapPx + contentWidth
        val height = if (stacked) labelHeight + gapPx + contentHeight else maxOf(labelHeight, contentHeight)
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height) {
            if (stacked) {
                labelPlaceables.forEach { it.placeRelative(0, 0) }
                contentPlaceables.forEach { it.placeRelative(0, labelHeight + gapPx) }
            } else {
                labelPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
                contentPlaceables.forEach { it.placeRelative(labelWidth + gapPx, (height - it.height) / 2) }
            }
        }
    }
}

@Composable
private fun controlLabels() = listOf(
    stringResource(R.string.console_label_plan),
    stringResource(R.string.console_label_friends),
    stringResource(R.string.console_label_delay),
    stringResource(R.string.console_label_fail)
)

@Composable
private fun rememberLabelWidth(labels: List<String> = controlLabels()): Dp {
    val style = OpalTheme.typography.caption
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, style, density) {
        with(density) { labels.maxOf { textMeasurer.measure(it, style, softWrap = false).size.width }.toDp() }
    }
}

private fun stacksLabels(labelWidth: Dp, rowWidth: Dp) = labelWidth > rowWidth / 3

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

@Composable
private fun Controls(
    debug: ReferralDebugState,
    isConfirmingReset: Boolean,
    onIntent: (ConsoleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = controlLabels()
    val format = rememberNumberFormat()
    val addDescription = stringResource(R.string.console_add_friend_description)
    val measuredLabelWidth = rememberLabelWidth(labels)
    OpalCard(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val labelWidth = measuredLabelWidth.takeUnless { stacksLabels(it, maxWidth) }
            Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                ControlRow(labels[0], labelWidth, group = true) {
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
                ControlRow(labels[1], labelWidth) {
                    val next = debug.nextTierAt
                    FriendCount(
                        count = debug.successfulReferrals,
                        format = format,
                        modifier = Modifier.padding(horizontal = OpalTheme.spacing.xxSmall)
                    )
                    OpalButton(
                        text = stringResource(R.string.console_add_friend),
                        onClick = { onIntent(ConsoleIntent.AddFriend) },
                        style = OpalButtonStyle.Outlined,
                        size = OpalButtonSize.Small,
                        enabled = debug.successfulReferrals < debug.maxReferrals,
                        modifier = Modifier.semantics { contentDescription = addDescription }
                    )
                    OpalButton(
                        text = if (next != null) {
                            pluralStringResource(R.plurals.console_reach_friends, next, format.format(next))
                        } else {
                            stringResource(R.string.console_all_unlocked)
                        },
                        onClick = { onIntent(ConsoleIntent.AdvanceToNextTier) },
                        style = OpalButtonStyle.Outlined,
                        size = OpalButtonSize.Small,
                        enabled = next != null
                    )
                }
                ControlRow(labels[2], labelWidth, group = true) {
                    SimulatedLatency.entries.forEach { latency ->
                        OpalChip(
                            text = latency.label(),
                            selected = debug.latency == latency,
                            onClick = { onIntent(ConsoleIntent.SetLatency(latency)) }
                        )
                    }
                }
                ControlRow(labels[3], labelWidth) {
                    OpalToggleChip(
                        text = stringResource(R.string.console_fail_next_load),
                        checked = debug.failNextLoad,
                        onCheckedChange = { onIntent(ConsoleIntent.ToggleFailNextLoad) }
                    )
                    OpalToggleChip(
                        text = stringResource(R.string.console_fail_next_claim),
                        checked = debug.failNextClaim,
                        onCheckedChange = { onIntent(ConsoleIntent.ToggleFailNextClaim) }
                    )
                }
                ResetButton(isConfirming = isConfirmingReset, onIntent = onIntent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlRow(label: String, labelWidth: Dp?, group: Boolean = false, content: @Composable () -> Unit) {
    LabeledRow(
        stacked = labelWidth == null,
        label = {
            Text(
                text = label,
                style = OpalTheme.typography.caption,
                color = OpalTheme.colors.contentSecondary,
                softWrap = labelWidth == null,
                modifier = if (labelWidth != null) Modifier.width(labelWidth) else Modifier
            )
        }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = if (group) Modifier.selectableGroup() else Modifier
        ) { content() }
    }
}

@Composable
private fun FriendCount(count: Int, format: NumberFormat, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.console_friends, count, format.format(count))
    val style = OpalTheme.typography.body.copy(fontFeatureSettings = "tnum")
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            val enter = slideInVertically(OpalMotion.SlideSpring) { it / 2 * direction } +
                fadeIn(OpalMotion.StateSpring)
            val exit = slideOutVertically(OpalMotion.SlideSpring) { -it / 2 * direction } +
                fadeOut(OpalMotion.StateSpring)
            enter togetherWith exit using SizeTransform(clip = false) { _, _ -> OpalMotion.SizeSpring }
        },
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        label = "friendCount"
    ) {
        Text(text = format.format(it), style = style)
    }
}

@Composable
private fun ResetButton(isConfirming: Boolean, onIntent: (ConsoleIntent) -> Unit, modifier: Modifier = Modifier) {
    val accessibilityManager = LocalAccessibilityManager.current
    val label = stringResource(if (isConfirming) R.string.console_reset_confirm else R.string.console_reset)
    val confirmDescription = stringResource(R.string.console_reset_confirm_state)
    val reset = {
        val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = RESET_CONFIRM_TIMEOUT.inWholeMilliseconds,
            containsText = true,
            containsControls = true
        )
        onIntent(ConsoleIntent.Reset(timeout?.milliseconds ?: RESET_CONFIRM_TIMEOUT))
    }
    AnimatedContent(
        targetState = isConfirming,
        transitionSpec = {
            fadeIn(ResetFadeIn) togetherWith fadeOut(ResetFadeOut) using
                SizeTransform(clip = false) { _, _ -> OpalMotion.SizeSpring }
        },
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.clearAndSetSemantics {
            role = Role.Button
            contentDescription = label
            if (isConfirming) stateDescription = confirmDescription
            liveRegion = LiveRegionMode.Polite
            onClick {
                reset()
                true
            }
        },
        label = "reset"
    ) { confirming ->
        OpalButton(
            text = stringResource(if (confirming) R.string.console_reset_confirm else R.string.console_reset),
            onClick = reset,
            style = if (confirming) OpalButtonStyle.Accent else OpalButtonStyle.Outlined,
            size = OpalButtonSize.Small
        )
    }
}

private const val RESET_FADE_OUT_MILLIS = 90
private val ResetFadeOut = tween<Float>(RESET_FADE_OUT_MILLIS, easing = LinearEasing)
private val ResetFadeIn = tween<Float>(OpalMotion.CrossFade.durationMillis, RESET_FADE_OUT_MILLIS, LinearEasing)

@Composable
private fun SimulatedLatency.label(): String = if (millis == 0L) {
    stringResource(R.string.console_delay_none)
} else {
    stringResource(R.string.console_delay_seconds, rememberNumberFormat(maxFractionDigits = 1).format(millis / 1000.0))
}

@Composable
private fun rememberNumberFormat(maxFractionDigits: Int = 0): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, maxFractionDigits) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = maxFractionDigits }
    }
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

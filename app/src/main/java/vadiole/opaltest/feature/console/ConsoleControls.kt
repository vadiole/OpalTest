package vadiole.opaltest.feature.console

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration.Companion.milliseconds
import vadiole.opaltest.R
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalChip
import vadiole.opaltest.ui.common.OpalToggleChip
import vadiole.opaltest.ui.common.formatCount
import vadiole.opaltest.ui.common.formatDecimal
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun LabeledRow(stacked: Boolean, label: @Composable () -> Unit, content: @Composable () -> Unit) {
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
internal fun controlLabels() = listOf(
    stringResource(R.string.console_label_plan),
    stringResource(R.string.console_label_friends),
    stringResource(R.string.console_label_delay),
    stringResource(R.string.console_label_fail)
)

@Composable
internal fun rememberLabelWidth(labels: List<String> = controlLabels()): Dp {
    val style = OpalTheme.typography.caption
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, style, density) {
        with(density) { labels.maxOf { textMeasurer.measure(it, style, softWrap = false).size.width }.toDp() }
    }
}

internal fun stacksLabels(labelWidth: Dp, rowWidth: Dp) = labelWidth > rowWidth / 3

@Composable
internal fun Controls(
    debug: ReferralDebugState,
    isConfirmingReset: Boolean,
    onIntent: (ConsoleIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = controlLabels()
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
                            pluralStringResource(R.plurals.console_reach_friends, next, next.formatCount())
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
private fun FriendCount(count: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.console_friends, count, count.formatCount())
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
        Text(text = it.formatCount(), style = style)
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
    stringResource(R.string.console_delay_seconds, (millis / 1000.0).formatDecimal(maxFractionDigits = 1))
}

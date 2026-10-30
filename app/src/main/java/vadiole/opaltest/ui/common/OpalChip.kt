package vadiole.opaltest.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    ChipSurface(
        text = text,
        selected = selected,
        enabled = enabled,
        interactionSource = interactionSource,
        interactionModifier = Modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.RadioButton
        ) {
            if (selected) return@selectable
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        modifier = modifier
    )
}

@Composable
fun OpalToggleChip(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    ChipSurface(
        text = text,
        selected = checked,
        enabled = enabled,
        interactionSource = interactionSource,
        interactionModifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox
        ) {
            haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onCheckedChange(it)
        },
        modifier = modifier
    )
}

@Composable
private fun ChipSurface(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    interactionModifier: Modifier = Modifier
) {
    val colors = OpalTheme.colors
    val indicationSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) colors.content else colors.fill, label = "chipBackground")
    val content by animateColorAsState(if (selected) colors.onAccent else colors.content, label = "chipContent")
    val alpha = animateEnabledAlpha(enabled)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = OpalTheme.layout.controlHeightSmall)
            .pressableSurface(
                interactionSource = interactionSource,
                indicationSource = indicationSource,
                shape = OpalTheme.shapes.pill,
                background = SolidColor(background),
                ripple = chipRipple(selected),
                enabled = enabled,
                alpha = { alpha.value }
            )
            .then(interactionModifier)
            .padding(horizontal = OpalTheme.spacing.small, vertical = OpalTheme.spacing.xxSmall)
    ) {
        OpalLabel(text = text, style = OpalTheme.typography.calloutMedium, color = content)
    }
}

private fun chipRipple(selected: Boolean) = if (selected) {
    opalRipple(OpalTheme.colors.onAccent, OpalTheme.alpha.rippleOnLight)
} else {
    opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalChipPreview() {
    OpalTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            OpalChip(text = "Free", selected = true, onClick = {})
            OpalChip(text = "Pro", selected = false, onClick = {})
            OpalChip(text = "Off", selected = false, enabled = false, onClick = {})
            OpalToggleChip(text = "Fail", checked = true, onCheckedChange = {})
        }
    }
}

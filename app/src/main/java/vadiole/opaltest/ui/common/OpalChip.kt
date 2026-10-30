package vadiole.opaltest.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    ChipSurface(
        text = text,
        selected = selected,
        enabled = enabled,
        interactionModifier = Modifier.selectable(selected = selected, enabled = enabled) {
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
    ChipSurface(
        text = text,
        selected = checked,
        enabled = enabled,
        interactionModifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox) {
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
    modifier: Modifier = Modifier,
    interactionModifier: Modifier = Modifier
) {
    val colors = OpalTheme.colors
    val background by animateColorAsState(if (selected) colors.content else colors.fill, label = "chipBackground")
    val content by animateColorAsState(if (selected) colors.onAccent else colors.content, label = "chipContent")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .alpha(if (enabled) 1f else OpalTheme.alpha.disabled)
            .clip(OpalTheme.shapes.pill)
            .background(background)
            .then(interactionModifier)
            .padding(horizontal = OpalTheme.spacing.small)
    ) {
        Text(text = text, style = OpalTheme.typography.calloutMedium, color = content, maxLines = 1)
    }
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

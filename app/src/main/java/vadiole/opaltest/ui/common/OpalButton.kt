package vadiole.opaltest.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

enum class OpalButtonStyle { Accent, Primary }

enum class OpalButtonSize { Large, Small }

@Composable
fun OpalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OpalButtonStyle = OpalButtonStyle.Accent,
    size: OpalButtonSize = OpalButtonSize.Large,
    @DrawableRes leadingIcon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = OpalTheme.colors
    val metrics = size.metrics()
    val background: Brush = when (style) {
        OpalButtonStyle.Accent -> colors.accent
        OpalButtonStyle.Primary -> SolidColor(colors.content)
    }
    val busyDescription = stringResource(R.string.opal_button_loading)
    val interactionSource = remember { MutableInteractionSource() }
    val indicationSource = remember { MutableInteractionSource() }
    val interactive = enabled && !loading
    val alpha = animateEnabledAlpha(enabled)
    val busy = animateFloatAsState(
        targetValue = if (loading) 1f else 0f,
        animationSpec = OpalMotion.StateSpring,
        label = "buttonBusy"
    )
    val spinnerVisible by remember { derivedStateOf { busy.value > 0f } }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics {
                if (loading) {
                    contentDescription = text
                    stateDescription = busyDescription
                }
            }
            .heightIn(min = metrics.height)
            .pressableSurface(
                interactionSource = interactionSource,
                indicationSource = indicationSource,
                shape = OpalTheme.shapes.pill,
                background = background,
                ripple = opalRipple(colors.onAccent, OpalTheme.alpha.rippleOnLight),
                enabled = interactive,
                alpha = { alpha.value }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = interactive,
                role = Role.Button,
                onClick = onClick
            )
            .padding(metrics.padding)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer { this.alpha = 1f - busy.value }
                .then(if (loading) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            if (leadingIcon != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(metrics.iconSize)) {
                    Icon(painter = painterResource(leadingIcon), contentDescription = null, tint = colors.onAccent)
                }
            }
            OpalLabel(text = text, style = metrics.textStyle, color = colors.onAccent)
        }
        if (loading || spinnerVisible) {
            CircularProgressIndicator(
                color = colors.onAccent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(metrics.iconSize)
                    .graphicsLayer {
                        val fraction = busy.value
                        val scale = lerp(OpalMotion.SPINNER_START_SCALE, 1f, fraction)
                        this.alpha = fraction
                        scaleX = scale
                        scaleY = scale
                    }
                    .clearAndSetSemantics {}
            )
        }
    }
}

@Composable
internal fun OpalLabel(text: String, style: TextStyle, color: Color) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2
    )
}

private class ButtonMetrics(val height: Dp, val padding: PaddingValues, val iconSize: Dp, val textStyle: TextStyle)

private fun OpalButtonSize.metrics(): ButtonMetrics = when (this) {
    OpalButtonSize.Large -> ButtonMetrics(
        height = OpalTheme.layout.controlHeight,
        padding = PaddingValues(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.xSmall),
        iconSize = OpalTheme.iconSize.medium,
        textStyle = OpalTheme.typography.body
    )

    OpalButtonSize.Small -> ButtonMetrics(
        height = OpalTheme.layout.controlHeightSmall,
        padding = PaddingValues(horizontal = OpalTheme.spacing.small, vertical = OpalTheme.spacing.xxSmall),
        iconSize = OpalTheme.iconSize.small,
        textStyle = OpalTheme.typography.calloutMedium
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalButtonPreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            OpalButton(
                text = "Share invite",
                onClick = {},
                leadingIcon = R.drawable.ic_share,
                modifier = Modifier.fillMaxWidth()
            )
            OpalButton(text = "Share invite", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                OpalButton(text = "Claim", onClick = {}, style = OpalButtonStyle.Primary, size = OpalButtonSize.Small)
                OpalButton(
                    text = "Claim",
                    onClick = {},
                    style = OpalButtonStyle.Primary,
                    size = OpalButtonSize.Small,
                    loading = true
                )
                OpalButton(
                    text = "Claim",
                    onClick = {},
                    style = OpalButtonStyle.Primary,
                    size = OpalButtonSize.Small,
                    enabled = false
                )
            }
        }
    }
}

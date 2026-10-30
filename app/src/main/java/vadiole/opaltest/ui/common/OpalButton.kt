package vadiole.opaltest.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(metrics.height)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(OpalTheme.shapes.pill)
            .background(background)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(metrics.padding)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(if (loading) 0f else 1f)
        ) {
            if (leadingIcon != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(metrics.iconSize)) {
                    Icon(painter = painterResource(leadingIcon), contentDescription = null, tint = colors.onAccent)
                }
            }
            Text(text = text, style = metrics.textStyle, color = colors.onAccent, maxLines = 1)
        }
        if (loading) {
            CircularProgressIndicator(
                color = colors.onAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(metrics.iconSize)
            )
        }
    }
}

internal const val DISABLED_ALPHA = 0.4f

private class ButtonMetrics(val height: Dp, val padding: PaddingValues, val iconSize: Dp, val textStyle: TextStyle)

private fun OpalButtonSize.metrics(): ButtonMetrics = when (this) {
    OpalButtonSize.Large -> ButtonMetrics(
        height = 50.dp,
        padding = PaddingValues(horizontal = OpalTheme.spacing.mediumLarge),
        iconSize = OpalTheme.iconSize.medium,
        textStyle = OpalTheme.typography.body
    )

    OpalButtonSize.Small -> ButtonMetrics(
        height = 40.dp,
        padding = PaddingValues(horizontal = OpalTheme.spacing.small),
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

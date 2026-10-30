package vadiole.opaltest.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indicationSource = remember { MutableInteractionSource() }
    val alpha = animateEnabledAlpha(enabled)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(OpalTheme.layout.iconButtonSize)
            .pressableSurface(
                interactionSource = interactionSource,
                indicationSource = indicationSource,
                shape = OpalTheme.shapes.pill,
                background = SolidColor(OpalTheme.colors.fill),
                ripple = opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill),
                enabled = enabled,
                alpha = { alpha.value }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OpalTheme.colors.content
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalIconButtonPreview() {
    OpalTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            OpalIconButton(icon = R.drawable.ic_arrow_back, contentDescription = null, onClick = {})
            OpalIconButton(icon = R.drawable.ic_arrow_back, contentDescription = null, onClick = {}, enabled = false)
        }
    }
}

package vadiole.opaltest.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indicationSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .pressFeedback(interactionSource, indicationSource)
            .clip(OpalTheme.shapes.pill)
            .background(OpalTheme.colors.fill)
            .indication(indicationSource, opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
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
        OpalIconButton(
            icon = R.drawable.ic_arrow_back,
            contentDescription = null,
            onClick = {},
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        )
    }
}

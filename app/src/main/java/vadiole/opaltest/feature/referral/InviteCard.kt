package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.outlineBorder
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun InviteCard(invitedBy: String, modifier: Modifier = Modifier) {
    InviteCardFrame(
        invitedBy = invitedBy,
        shape = OpalTheme.shapes.large,
        spacing = OpalTheme.spacing.medium,
        padding = PaddingValues(horizontal = OpalTheme.spacing.medium),
        modifier = modifier.height(ReferralDimens.inviteCardHeight)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_opal_logo),
            contentDescription = stringResource(R.string.referral_opal_logo),
            tint = OpalTheme.colors.content
        )
    }
}

@Composable
internal fun CompactInviteCard(invitedBy: String, modifier: Modifier = Modifier) {
    InviteCardFrame(
        invitedBy = invitedBy,
        shape = OpalTheme.shapes.xLarge,
        spacing = OpalTheme.spacing.small,
        padding = PaddingValues(horizontal = OpalTheme.spacing.medium, vertical = OpalTheme.spacing.small),
        modifier = modifier.heightIn(min = ReferralDimens.compactInviteCardHeight)
    )
}

@Composable
private fun InviteCardFrame(
    invitedBy: String,
    shape: Shape,
    spacing: Dp,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {}
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .outlineBorder(OpalTheme.layout.borderWidth, OpalTheme.colors.borderSubtle, shape)
    ) {
        OpalImage(
            id = R.drawable.img_invite_card_background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.padding(padding)
        ) {
            header()
            OpalImage(
                id = R.drawable.img_avatar_placeholder,
                contentDescription = null,
                modifier = Modifier.size(ReferralDimens.avatarSize)
            )
            Text(
                text = stringResource(R.string.referral_invited_by, invitedBy),
                style = OpalTheme.typography.caption,
                color = OpalTheme.colors.contentHigh,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(widthDp = 402, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InviteCardLongNamePreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            InviteCard("Kenneth", Modifier.fillMaxWidth())
            InviteCard("Maximilian Alexander von Hohenzollern-Sigmaringen", Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactInviteCardPreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            CompactInviteCard("Kenneth", Modifier.fillMaxWidth())
            CompactInviteCard("Maximilian Alexander von Hohenzollern-Sigmaringen", Modifier.fillMaxWidth())
        }
    }
}

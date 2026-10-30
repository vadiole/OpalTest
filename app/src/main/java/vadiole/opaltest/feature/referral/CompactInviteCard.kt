package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.outlineBorder
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun CompactInviteCard(invitedBy: String?, modifier: Modifier = Modifier) {
    val shape = OpalTheme.shapes.xLarge
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = ReferralDimens.compactInviteCardHeight)
            .clip(shape)
            .outlineBorder(1.dp, OpalTheme.colors.borderSubtle, shape)
    ) {
        OpalImage(
            id = R.drawable.img_invite_card_background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.padding(horizontal = OpalTheme.spacing.medium, vertical = OpalTheme.spacing.small)
        ) {
            OpalImage(
                id = R.drawable.img_avatar_placeholder,
                contentDescription = null,
                modifier = Modifier.size(ReferralDimens.avatarSize)
            )
            if (invitedBy != null) {
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

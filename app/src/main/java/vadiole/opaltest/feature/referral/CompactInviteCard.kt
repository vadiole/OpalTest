package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.outlineBorder
import vadiole.opaltest.ui.theme.OpalTheme

private enum class CompactInviteLayout { Aligned, Medallion, IdCard }

private val ChosenLayout = CompactInviteLayout.Aligned

@Composable
internal fun CompactInviteCard(invitedBy: String?, modifier: Modifier = Modifier) {
    CompactInviteCard(invitedBy, ChosenLayout, modifier)
}

@Composable
private fun CompactInviteCard(invitedBy: String?, layout: CompactInviteLayout, modifier: Modifier = Modifier) {
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
        when (layout) {
            CompactInviteLayout.Aligned -> AlignedInvite(invitedBy)
            CompactInviteLayout.Medallion -> MedallionInvite(invitedBy)
            CompactInviteLayout.IdCard -> IdCardInvite(invitedBy)
        }
    }
}

@Composable
private fun AlignedInvite(invitedBy: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(OpalTheme.spacing.small)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ReferralDimens.tierArtworkSize)) {
            Avatar()
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            modifier = Modifier.weight(1f)
        ) {
            Logo()
            InvitedBy(invitedBy, TextAlign.Start)
        }
    }
}

@Composable
private fun MedallionInvite(invitedBy: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
        modifier = Modifier.padding(horizontal = OpalTheme.spacing.medium, vertical = OpalTheme.spacing.small)
    ) {
        Avatar()
        InvitedBy(invitedBy, TextAlign.Center)
    }
}

@Composable
private fun IdCardInvite(invitedBy: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OpalTheme.spacing.large, vertical = OpalTheme.spacing.small)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ReferralDimens.avatarSize)
        ) {
            Logo()
            InvitedBy(invitedBy, TextAlign.Start)
        }
        Avatar()
    }
}

@Composable
private fun Logo() {
    Icon(
        painter = painterResource(R.drawable.ic_opal_logo),
        contentDescription = stringResource(R.string.referral_opal_logo),
        tint = OpalTheme.colors.content
    )
}

@Composable
private fun Avatar() {
    OpalImage(
        id = R.drawable.img_avatar_placeholder,
        contentDescription = null,
        modifier = Modifier.size(ReferralDimens.avatarSize)
    )
}

@Composable
private fun InvitedBy(invitedBy: String?, textAlign: TextAlign) {
    if (invitedBy != null) {
        Text(
            text = stringResource(R.string.referral_invited_by, invitedBy),
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentHigh,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactInvitePreview(layout: CompactInviteLayout) {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            CompactInviteCard("Kenneth", layout, Modifier.fillMaxWidth())
            CompactInviteCard("Maximilian Alexander von Hohenzollern-Sigmaringen", layout, Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactInviteAlignedPreview() = CompactInvitePreview(CompactInviteLayout.Aligned)

@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactInviteMedallionPreview() = CompactInvitePreview(CompactInviteLayout.Medallion)

@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactInviteIdCardPreview() = CompactInvitePreview(CompactInviteLayout.IdCard)

package vadiole.opaltest.feature.referral

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import vadiole.opaltest.ui.theme.OpalLayout
import vadiole.opaltest.ui.theme.OpalSpacing

internal object ReferralDimens {
    val contentTop = OpalLayout.topBarHeight + OpalSpacing.xxSmall
    val inviteCardHeight = 194.dp
    val avatarSize = 64.dp
    val codeCell = DpSize(44.dp, 56.dp)
    val codeCellGap = 6.dp
    val tierArtworkSize = 100.dp
    val compactTierArtworkSize = 64.dp
    val minTierTextWidth = 150.dp
    val claimedIconSize = 13.dp
    val tierConnectorHeight = 32.dp
    val compactInviteCardHeight = tierArtworkSize + OpalSpacing.small * 2
    val progressMaxWidth = 179.dp
}

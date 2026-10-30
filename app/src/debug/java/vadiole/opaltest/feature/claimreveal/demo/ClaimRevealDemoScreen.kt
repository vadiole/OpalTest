package vadiole.opaltest.feature.claimreveal.demo

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.feature.claimreveal.ClaimRevealHost
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.RevealContent
import vadiole.opaltest.feature.claimreveal.claimRevealSource
import vadiole.opaltest.feature.claimreveal.pressScale
import vadiole.opaltest.feature.claimreveal.rememberClaimRevealState
import vadiole.opaltest.feature.claimreveal.revealImage
import vadiole.opaltest.feature.claimreveal.thumbnail
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalProgressBar
import vadiole.opaltest.ui.theme.OpalTheme

private data class DemoTier(val stone: MilestoneStone, val friends: Int, val name: Int)

private val demoTiers = listOf(
    DemoTier(MilestoneStone.Loyal, 1, R.string.claim_reveal_demo_loyal),
    DemoTier(MilestoneStone.Soulful, 3, R.string.claim_reveal_demo_soulful),
    DemoTier(MilestoneStone.Popular, 10, R.string.claim_reveal_demo_popular)
)

@Composable
fun ClaimRevealDemoScreen(modifier: Modifier = Modifier) {
    val reveal = rememberClaimRevealState()
    var claimed by rememberSaveable { mutableStateOf(emptyList<MilestoneStone>()) }
    ClaimRevealHost(
        state = reveal,
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .safeDrawingPadding()
                    .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.medium)
            ) {
                Text(stringResource(R.string.claim_reveal_demo_title), style = OpalTheme.typography.headline)
                demoTiers.forEach { tier ->
                    val name = stringResource(tier.name)
                    DemoTierCard(
                        tier = tier,
                        name = name,
                        claimed = tier.stone in claimed && !reveal.isHolding(tier.stone),
                        reveal = reveal,
                        onClaim = {
                            claimed = claimed + tier.stone
                            reveal.open(tier.stone, RevealContent(gem = tier.stone.revealImage, title = name))
                        }
                    )
                }
                OpalButton(
                    text = stringResource(R.string.claim_reveal_demo_reset),
                    onClick = { claimed = emptyList() },
                    style = OpalButtonStyle.Primary,
                    size = OpalButtonSize.Small,
                    modifier = Modifier.pressScale()
                )
            }
        }
    }
}

@Composable
private fun DemoTierCard(
    tier: DemoTier,
    name: String,
    claimed: Boolean,
    reveal: ClaimRevealState,
    onClaim: () -> Unit
) {
    OpalCard(
        Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
            .alpha(if (claimed) 0.75f else 1f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(OpalTheme.shapes.medium)
                    .background(OpalTheme.colors.fillSubtle)
            ) {
                Image(
                    painter = painterResource(tier.stone.thumbnail),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .claimRevealSource(reveal, tier.stone)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                    if (claimed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = OpalTheme.colors.accentStart,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.claim_reveal_demo_claimed),
                                style = OpalTheme.typography.caption.copy(brush = OpalTheme.colors.accent)
                            )
                        }
                    } else {
                        Text(
                            text = pluralStringResource(
                                R.plurals.claim_reveal_demo_friends,
                                tier.friends,
                                tier.friends
                            ),
                            style = OpalTheme.typography.caption.copy(brush = OpalTheme.colors.accent)
                        )
                    }
                    Text(name, style = OpalTheme.typography.body)
                    Text(
                        text = stringResource(R.string.claim_reveal_demo_subtitle),
                        style = OpalTheme.typography.caption,
                        color = OpalTheme.colors.contentSecondary
                    )
                }
                if (claimed) {
                    OpalProgressBar(progress = 1f, modifier = Modifier.fillMaxWidth())
                } else {
                    OpalButton(
                        text = stringResource(R.string.claim_reveal_demo_claim),
                        onClick = onClaim,
                        style = OpalButtonStyle.Primary,
                        size = OpalButtonSize.Small,
                        modifier = Modifier.pressScale()
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ClaimRevealDemoScreenPreview() {
    OpalTheme {
        ClaimRevealDemoScreen()
    }
}

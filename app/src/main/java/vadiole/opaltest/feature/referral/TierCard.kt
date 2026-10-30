package vadiole.opaltest.feature.referral

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.RewardTier
import vadiole.opaltest.domain.referral.TierStatus
import vadiole.opaltest.domain.referral.progress
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.ClaimedGemGlow
import vadiole.opaltest.feature.claimreveal.claimRevealSource
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.OpalProgressBar
import vadiole.opaltest.ui.common.brushTint
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun TierCard(
    tier: RewardTier,
    successfulReferrals: Int,
    claiming: Boolean,
    claimable: Boolean,
    active: Boolean,
    reveal: ClaimRevealState,
    onClaim: () -> Unit
) {
    val featured = tier.reward.isFreeYear
    val stone = (tier.reward as? Reward.Milestone)?.stone
    val status = if (stone != null && reveal.isHolding(stone)) TierStatus.Claimable else tier.status
    val alpha by animateFloatAsState(
        targetValue = if (status == TierStatus.Claimed) OpalTheme.alpha.completed else 1f,
        animationSpec = OpalMotion.StateSpring,
        label = "tierAlpha"
    )
    OpalCard(
        highlighted = active,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TierArtwork(tier.reward, featured, claimed = status == TierStatus.Claimed, reveal = reveal)
            Column(
                verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = OpalTheme.spacing.xxSmall)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                    TierCaption(tier, status)
                    Column {
                        Text(text = tier.reward.title(), style = OpalTheme.typography.body)
                        Text(
                            text = tier.reward.description(),
                            style = OpalTheme.typography.caption,
                            color = OpalTheme.colors.contentSecondary
                        )
                    }
                }
                AnimatedContent(
                    targetState = status == TierStatus.Claimable,
                    transitionSpec = {
                        (fadeIn(OpalMotion.StateSpring) togetherWith fadeOut(OpalMotion.StateSpring))
                            .using(SizeTransform(clip = false) { _, _ -> OpalMotion.SizeSpring })
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "tierAction"
                ) { showClaim ->
                    if (showClaim) {
                        val exiting = transition.targetState != EnterExitState.Visible
                        OpalButton(
                            text = stringResource(R.string.referral_claim),
                            onClick = onClaim,
                            style = OpalButtonStyle.Primary,
                            size = OpalButtonSize.Small,
                            enabled = claimable,
                            loading = claiming || exiting
                        )
                    } else {
                        OpalProgressBar(
                            progress = tier.progress(successfulReferrals),
                            modifier = Modifier
                                .widthIn(max = 179.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierCaption(tier: RewardTier, status: TierStatus) {
    val style = OpalTheme.typography.caption.copy(brush = OpalTheme.colors.accent)
    AnimatedContent(
        targetState = status == TierStatus.Claimed,
        transitionSpec = {
            (fadeIn(OpalMotion.StateSpring) + slideInVertically(OpalMotion.SlideSpring) { it / 2 }) togetherWith
                (fadeOut(OpalMotion.StateSpring) + slideOutVertically(OpalMotion.SlideSpring) { -it / 2 })
        },
        label = "tierCaption"
    ) { claimed ->
        if (claimed) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val entering = transition.currentState == EnterExitState.PreEnter
                val scale = remember { Animatable(if (entering) OpalMotion.POP_START_SCALE else 1f) }
                LaunchedEffect(Unit) { scale.animateTo(1f, OpalMotion.PopSpring) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        modifier = Modifier.brushTint(OpalTheme.colors.accent)
                    )
                }
                Text(text = stringResource(R.string.referral_claimed), style = style)
            }
        } else {
            Text(
                text = pluralStringResource(
                    R.plurals.referral_tier_friends,
                    tier.requiredReferrals,
                    tier.requiredReferrals
                ),
                style = style
            )
        }
    }
}

@Composable
private fun TierArtwork(reward: Reward, featured: Boolean, claimed: Boolean, reveal: ClaimRevealState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .clip(OpalTheme.shapes.medium)
            .then(if (featured) Modifier else Modifier.background(OpalTheme.colors.fillSubtle))
    ) {
        val stone = (reward as? Reward.Milestone)?.stone
        if (stone != null) {
            ClaimedGemGlow(gem = stone.thumbnail, visible = claimed, modifier = Modifier.fillMaxSize())
        }
        val image = when (reward) {
            is Reward.Milestone -> reward.stone.thumbnail
            is Reward.ProAccess -> if (reward.isFreeYear) R.drawable.img_reward_pro_year else null
            Reward.Mystery -> null
        }
        if (image != null) {
            OpalImage(
                id = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (stone != null) Modifier.claimRevealSource(reveal, stone) else Modifier)
            )
        } else {
            Icon(
                painter = painterResource(
                    if (reward == Reward.Mystery) R.drawable.ic_question_mark else R.drawable.ic_gift
                ),
                contentDescription = null,
                tint = OpalTheme.colors.content
            )
        }
    }
}

package vadiole.opaltest.feature.referral

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Currency
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.domain.referral.Fulfillment
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.domain.referral.Money
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ProTerm
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.domain.referral.RewardTier
import vadiole.opaltest.domain.referral.TierStatus
import vadiole.opaltest.domain.referral.progress
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalGlow
import vadiole.opaltest.ui.common.OpalIconButton
import vadiole.opaltest.ui.common.OpalProgressBar
import vadiole.opaltest.ui.common.brushTint
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ReferralScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferralViewModel = viewModel { ReferralViewModel(container.referralRepository) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReferralEffect.Share -> {
                    val message = resources.getQuantityString(
                        R.plurals.referral_share_message,
                        effect.trialDays,
                        effect.code,
                        effect.trialDays,
                        effect.url
                    )
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
                    context.startActivity(Intent.createChooser(send, null))
                }

                ReferralEffect.ClaimSucceeded -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)

                is ReferralEffect.ClaimFailed -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    val message = when (effect.error) {
                        ReferralError.Network -> R.string.referral_claim_failed_network
                        else -> R.string.referral_claim_failed
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    ReferralContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@Composable
fun ReferralContent(
    state: ReferralState,
    onIntent: (ReferralIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(R.string.referral_title)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
            .semantics { paneTitle = title }
    ) {
        OpalGlow(Modifier.fillMaxWidth())
        val program = state.program
        when {
            program != null -> Program(program, state.claimingTierId, onIntent)

            state.loadError != null -> LoadError(onRetry = { onIntent(ReferralIntent.Retry) })

            else -> CircularProgressIndicator(
                color = OpalTheme.colors.content,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        OpalIconButton(
            icon = R.drawable.ic_arrow_back,
            contentDescription = stringResource(R.string.referral_back),
            onClick = onBack,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.small)
        )
    }
}

@Composable
private fun Program(program: ReferralProgram, claimingTierId: String?, onIntent: (ReferralIntent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(
                start = OpalTheme.spacing.mediumLarge,
                end = OpalTheme.spacing.mediumLarge,
                top = 72.dp,
                bottom = OpalTheme.spacing.medium
            )
            .wrapContentWidth()
            .widthIn(max = OpalTheme.layout.maxContentWidth)
    ) {
        InviteCard(program.invitedBy)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)
        ) {
            Summary(program)
            Code(program.code)
        }
        OpalButton(
            text = stringResource(R.string.referral_share),
            onClick = { onIntent(ReferralIntent.Share) },
            leadingIcon = R.drawable.ic_share,
            modifier = Modifier.fillMaxWidth()
        )
        Tiers(program, claimingTierId, onIntent)
    }
}

@Composable
private fun InviteCard(invitedBy: String?) {
    val shape = OpalTheme.shapes.large
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(194.dp)
            .clip(shape)
            .border(1.dp, OpalTheme.colors.borderSubtle, shape)
    ) {
        Image(
            painter = painterResource(R.drawable.img_invite_card_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opal_logo),
                contentDescription = stringResource(R.string.referral_opal_logo),
                tint = OpalTheme.colors.content
            )
            Image(painter = painterResource(R.drawable.img_avatar_placeholder), contentDescription = null)
            if (invitedBy != null) {
                Text(
                    text = stringResource(R.string.referral_invited_by, invitedBy),
                    style = OpalTheme.typography.caption,
                    modifier = Modifier.alpha(0.9f)
                )
            }
        }
    }
}

@Composable
private fun Summary(program: ReferralProgram) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.referral_gift_days,
                program.inviteeTrialDays,
                program.inviteeTrialDays
            ),
            style = OpalTheme.typography.headline,
            textAlign = TextAlign.Center
        )
        val referrals = program.successfulReferrals
        val friends = pluralStringResource(R.plurals.referral_friends, referrals, referrals)
        val template = stringResource(R.string.referral_referred, PLACEHOLDER)
        val accent = OpalTheme.colors.accent
        Text(
            text = if (referrals == 0) {
                AnnotatedString(stringResource(R.string.referral_referred_none))
            } else {
                buildAnnotatedString {
                    append(template.substringBefore(PLACEHOLDER))
                    withStyle(SpanStyle(brush = accent)) { append(friends) }
                    append(template.substringAfter(PLACEHOLDER))
                }
            },
            style = OpalTheme.typography.callout,
            textAlign = TextAlign.Center
        )
    }
}

private const val PLACEHOLDER = "\u0000"

@Composable
private fun Code(code: String) {
    val title = stringResource(R.string.referral_code_title)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$title: ${code.toList().joinToString(" ")}" }
    ) {
        Text(text = title, style = OpalTheme.typography.headline)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            code.forEach { char ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 44.dp, height = 56.dp)
                        .clip(OpalTheme.shapes.small)
                        .background(OpalTheme.colors.fill)
                ) {
                    Text(text = char.toString(), style = OpalTheme.typography.largeTitle)
                }
            }
        }
    }
}

@Composable
private fun Tiers(program: ReferralProgram, claimingTierId: String?, onIntent: (ReferralIntent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall)
    ) {
        program.tiers.forEachIndexed { index, tier ->
            if (index > 0) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_south),
                        contentDescription = null,
                        tint = OpalTheme.colors.content.copy(alpha = 0.4f)
                    )
                }
            }
            TierCard(
                tier = tier,
                successfulReferrals = program.successfulReferrals,
                claiming = tier.id == claimingTierId,
                onClaim = { onIntent(ReferralIntent.Claim(tier.id)) }
            )
        }
    }
}

@Composable
private fun TierCard(tier: RewardTier, successfulReferrals: Int, claiming: Boolean, onClaim: () -> Unit) {
    val featured = tier.reward.isFreeYear
    OpalCard(
        highlighted = featured,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (tier.status == TierStatus.Claimed) 0.75f else 1f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TierArtwork(tier.reward, featured)
            Column(
                verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = OpalTheme.spacing.xxSmall)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                    TierCaption(tier)
                    Column {
                        Text(text = tier.reward.title(), style = OpalTheme.typography.body)
                        Text(
                            text = tier.reward.description(),
                            style = OpalTheme.typography.caption,
                            color = OpalTheme.colors.contentSecondary
                        )
                    }
                }
                if (tier.status == TierStatus.Claimable) {
                    OpalButton(
                        text = stringResource(R.string.referral_claim),
                        onClick = onClaim,
                        style = OpalButtonStyle.Primary,
                        size = OpalButtonSize.Small,
                        loading = claiming
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

@Composable
private fun TierCaption(tier: RewardTier) {
    val style = OpalTheme.typography.caption.copy(brush = OpalTheme.colors.accent)
    if (tier.status == TierStatus.Claimed) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(13.dp)) {
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

@Composable
private fun TierArtwork(reward: Reward, featured: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .clip(OpalTheme.shapes.medium)
            .background(if (featured) Color.Transparent else OpalTheme.colors.fillSubtle)
    ) {
        val image = when (reward) {
            is Reward.Milestone -> when (reward.stone) {
                MilestoneStone.Loyal -> R.drawable.img_milestone_loyal
                MilestoneStone.Soulful -> R.drawable.img_milestone_soulful
                MilestoneStone.Popular -> R.drawable.img_milestone_popular
            }

            is Reward.ProAccess -> if (reward.isFreeYear) R.drawable.img_reward_pro_year else null

            Reward.Mystery -> null
        }
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
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

private val Reward.isFreeYear: Boolean
    get() = this is Reward.ProAccess && term == ProTerm.OneYear

@Composable
private fun Reward.title(): String = stringResource(
    when (this) {
        is Reward.Milestone -> when (stone) {
            MilestoneStone.Loyal -> R.string.referral_milestone_loyal
            MilestoneStone.Soulful -> R.string.referral_milestone_soulful
            MilestoneStone.Popular -> R.string.referral_milestone_popular
        }

        is Reward.ProAccess -> when (term) {
            ProTerm.OneYear -> R.string.referral_pro_one_year
            ProTerm.TwoYears -> R.string.referral_pro_two_years
            ProTerm.Lifetime -> R.string.referral_pro_lifetime
        }

        Reward.Mystery -> R.string.referral_mystery
    }
)

@Composable
private fun Reward.description(): String = when (this) {
    is Reward.Milestone -> stringResource(R.string.referral_milestone_description)

    is Reward.ProAccess -> {
        val text = stringResource(
            when (term) {
                ProTerm.OneYear -> R.string.referral_pro_one_year_description
                ProTerm.TwoYears -> R.string.referral_pro_two_years_description
                ProTerm.Lifetime -> R.string.referral_pro_lifetime_description
            }
        )
        value?.let { stringResource(R.string.referral_with_value, text, it.format()) } ?: text
    }

    Reward.Mystery -> stringResource(R.string.referral_mystery_description)
}

private fun Money.format(): String {
    val currency = Currency.getInstance(currencyCode)
    val amount = amountMinor.toBigDecimal().movePointLeft(currency.defaultFractionDigits)
    return NumberFormat.getCurrencyInstance().apply {
        this.currency = currency
        if (amount.stripTrailingZeros().scale() <= 0) maximumFractionDigits = 0
    }.format(amount)
}

@Composable
private fun LoadError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(OpalTheme.spacing.mediumLarge)
    ) {
        Text(
            text = stringResource(R.string.referral_load_failed),
            style = OpalTheme.typography.body,
            textAlign = TextAlign.Center
        )
        OpalButton(
            text = stringResource(R.string.referral_retry),
            onClick = onRetry,
            style = OpalButtonStyle.Primary,
            size = OpalButtonSize.Small
        )
    }
}

internal fun previewProgram(plan: Plan = Plan.Free, referrals: Int = 3): ReferralProgram {
    fun tier(id: String, required: Int, reward: Reward, claimed: Boolean = false) = RewardTier(
        id = id,
        requiredReferrals = required,
        reward = reward,
        status = when {
            claimed -> TierStatus.Claimed
            referrals >= required -> TierStatus.Claimable
            else -> TierStatus.Locked
        }
    )
    return ReferralProgram(
        code = "FR333",
        shareUrl = "https://opal.so/invite/FR333",
        plan = plan,
        successfulReferrals = referrals,
        inviteeTrialDays = 30,
        invitedBy = "Kenneth",
        tiers = listOfNotNull(
            tier("loyal", 1, Reward.Milestone(MilestoneStone.Loyal), claimed = referrals >= 1),
            tier("soulful", 3, Reward.Milestone(MilestoneStone.Soulful)),
            tier("year", 5, Reward.ProAccess(ProTerm.OneYear, Fulfillment.Instant, null))
                .takeIf { plan == Plan.Free },
            tier("popular", 10, Reward.Milestone(MilestoneStone.Popular)),
            tier("two_years", 20, Reward.ProAccess(ProTerm.TwoYears, Fulfillment.ContactSupport, Money(19900, "USD"))),
            tier("lifetime", 50, Reward.ProAccess(ProTerm.Lifetime, Fulfillment.ContactSupport, Money(39900, "USD"))),
            tier("mystery", 100, Reward.Mystery)
        )
    )
}

@Preview(widthDp = 402, heightDp = 1861)
@Composable
private fun ReferralContentFigmaPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState(program = previewProgram()),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ReferralContentProPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState(
                program = previewProgram(plan = Plan.Pro, referrals = 0)
            ),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(widthDp = 874, heightDp = 402)
@Composable
private fun ReferralContentLandscapePreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState(program = previewProgram(), claimingTierId = "soulful"),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ReferralContentTabletPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState(program = previewProgram(referrals = 12)),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ReferralContentLoadingPreview() {
    OpalTheme {
        ReferralContent(state = ReferralState(), onIntent = {}, onBack = {})
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ReferralContentErrorPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState(loadError = ReferralError.Network),
            onIntent = {},
            onBack = {}
        )
    }
}

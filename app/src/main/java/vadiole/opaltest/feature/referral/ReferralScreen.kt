package vadiole.opaltest.feature.referral

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.text.NumberFormat
import java.util.Currency
import kotlinx.coroutines.delay
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
import vadiole.opaltest.feature.claimreveal.ClaimRevealHost
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.ClaimedGemGlow
import vadiole.opaltest.feature.claimreveal.RevealContent
import vadiole.opaltest.feature.claimreveal.claimRevealSource
import vadiole.opaltest.feature.claimreveal.rememberClaimRevealState
import vadiole.opaltest.feature.claimreveal.revealImage
import vadiole.opaltest.feature.claimreveal.thumbnail
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalGlow
import vadiole.opaltest.ui.common.OpalIconButton
import vadiole.opaltest.ui.common.OpalImage
import vadiole.opaltest.ui.common.OpalProgressBar
import vadiole.opaltest.ui.common.OpalSkeleton
import vadiole.opaltest.ui.common.OpalTopBlur
import vadiole.opaltest.ui.common.SkeletonBlock
import vadiole.opaltest.ui.common.SkeletonLine
import vadiole.opaltest.ui.common.brushTint
import vadiole.opaltest.ui.common.opalRipple
import vadiole.opaltest.ui.common.outlineBorder
import vadiole.opaltest.ui.common.pressFeedback
import vadiole.opaltest.ui.common.rememberTopBlurVisibility
import vadiole.opaltest.ui.theme.OpalLayout
import vadiole.opaltest.ui.theme.OpalMotion
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
    val clipboard = LocalClipboard.current
    val reveal = rememberClaimRevealState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReferralEffect.Share -> {
                    val message = resources.inviteMessage(effect.invite)
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
                    context.startActivity(Intent.createChooser(send, null))
                }

                is ReferralEffect.CopyInvite -> {
                    val message = resources.inviteMessage(effect.invite)
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(effect.invite.code, message)))
                }

                is ReferralEffect.ClaimSucceeded -> {
                    val reward = effect.reward
                    if (reward is Reward.Milestone) {
                        val content = RevealContent(
                            gem = reward.stone.revealImage,
                            title = resources.getString(reward.stone.title)
                        )
                        reveal.open(reward.stone, content)
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }

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
    ReferralContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
        reveal = reveal
    )
}

private fun Resources.inviteMessage(invite: Invite): String =
    getQuantityString(R.plurals.referral_share_message, invite.trialDays, invite.code, invite.trialDays, invite.url)

@Composable
fun ReferralContent(
    state: ReferralState,
    onIntent: (ReferralIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reveal: ClaimRevealState = rememberClaimRevealState()
) {
    val title = stringResource(R.string.referral_title)
    val hazeState = rememberHazeState()
    val twoPane = isTwoPane()
    val listState = rememberLazyListState()
    val headerState = rememberLazyListState()
    val tiersState = rememberLazyListState()
    val listBlur by rememberTopBlurVisibility(listState)
    val headerBlur by rememberTopBlurVisibility(headerState)
    val tiersBlur by rememberTopBlurVisibility(tiersState)
    val skeletonListState = rememberScrollState()
    val skeletonHeaderState = rememberScrollState()
    val program = state.program
    ClaimRevealHost(
        state = reveal,
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .hazeSource(hazeState)
                    .background(OpalTheme.colors.background)
            ) {
                OpalGlow(Modifier.fillMaxWidth())
                val body = rememberBody(
                    when {
                        program != null -> Body.Program
                        state.loadError != null -> Body.Error
                        else -> Body.Loading
                    }
                )
                LaunchedEffect(body) {
                    when (body) {
                        Body.Loading -> {
                            skeletonListState.scrollTo(0)
                            skeletonHeaderState.scrollTo(0)
                        }

                        Body.Program -> if (twoPane) {
                            tiersState.scrollBy(skeletonListState.value.toFloat())
                            headerState.scrollBy(skeletonHeaderState.value.toFloat())
                        } else {
                            listState.scrollBy(skeletonListState.value.toFloat())
                        }

                        else -> Unit
                    }
                }
                AnimatedContent(
                    targetState = body,
                    transitionSpec = {
                        if (initialState == Body.Blank) {
                            EnterTransition.None togetherWith ExitTransition.None using null
                        } else {
                            fadeIn(OpalMotion.CrossFade) togetherWith fadeOut(OpalMotion.CrossFade) using null
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "body"
                ) {
                    when (it) {
                        Body.Program -> program?.let { program ->
                            Program(
                                program = program,
                                claimingTierId = state.claimingTierId,
                                reveal = reveal,
                                onIntent = onIntent,
                                twoPane = twoPane,
                                listState = listState,
                                headerState = headerState,
                                tiersState = tiersState
                            )
                        }

                        Body.Error -> LoadError(onRetry = { onIntent(ReferralIntent.Retry) })

                        Body.Loading -> Loading(headerState = skeletonHeaderState, listState = skeletonListState)

                        Body.Blank -> Unit
                    }
                }
            }
            OpalTopBlur(
                state = hazeState,
                visibility = {
                    when {
                        program == null -> 0f
                        twoPane -> maxOf(headerBlur, tiersBlur)
                        else -> listBlur
                    }
                },
                height = OpalTheme.spacing.small * 2 + OpalTheme.layout.iconButtonSize
            )
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
}

private enum class Pane { Single, Start, End }

@Composable
private fun isTwoPane(): Boolean = currentWindowAdaptiveInfo().windowSizeClass
    .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

@Composable
private fun paneContentPadding(pane: Pane): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val spacing = OpalTheme.spacing
    val gap = spacing.xLarge / 2
    return PaddingValues(
        start = if (pane == Pane.End) gap else insets.calculateStartPadding(layoutDirection) + spacing.mediumLarge,
        end = if (pane == Pane.Start) gap else insets.calculateEndPadding(layoutDirection) + spacing.mediumLarge,
        top = insets.calculateTopPadding() + 72.dp,
        bottom = insets.calculateBottomPadding() + spacing.medium
    )
}

private val paneItemModifier = Modifier
    .widthIn(max = OpalLayout.maxContentWidth)
    .fillMaxWidth()

@Composable
private fun Program(
    program: ReferralProgram,
    claimingTierId: String?,
    reveal: ClaimRevealState,
    onIntent: (ReferralIntent) -> Unit,
    twoPane: Boolean,
    listState: LazyListState,
    headerState: LazyListState,
    tiersState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (twoPane) {
        Row(modifier.fillMaxSize()) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paneContentPadding(Pane.Start),
                state = headerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                programHeader(program, onIntent)
            }
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = paneContentPadding(Pane.End),
                state = tiersState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                tiers(program, claimingTierId, reveal, onIntent, firstTopPadding = 0.dp)
            }
        }
    } else {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = paneContentPadding(Pane.Single),
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            programHeader(program, onIntent)
            tiers(program, claimingTierId, reveal, onIntent, firstTopPadding = OpalTheme.spacing.xLarge)
        }
    }
}

private enum class HeaderItem { Invite, Code, Share }

private fun LazyListScope.programHeader(program: ReferralProgram, onIntent: (ReferralIntent) -> Unit) {
    item(key = HeaderItem.Invite) {
        InviteCard(program.invitedBy, paneItemModifier)
    }
    item(key = HeaderItem.Code) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            modifier = paneItemModifier.padding(top = OpalTheme.spacing.xLarge)
        ) {
            Summary(program)
            Code(program.code, onCopy = { onIntent(ReferralIntent.CopyInvite) })
        }
    }
    item(key = HeaderItem.Share) {
        OpalButton(
            text = stringResource(R.string.referral_share),
            onClick = { onIntent(ReferralIntent.Share) },
            leadingIcon = R.drawable.ic_share,
            modifier = paneItemModifier.padding(top = OpalTheme.spacing.xLarge)
        )
    }
}

private fun LazyListScope.tiers(
    program: ReferralProgram,
    claimingTierId: String?,
    reveal: ClaimRevealState,
    onIntent: (ReferralIntent) -> Unit,
    firstTopPadding: Dp
) {
    val activeTierId = program.tiers.firstOrNull { it.status != TierStatus.Claimed }?.id
    itemsIndexed(program.tiers, key = { _, tier -> tier.id }) { index, tier ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
            modifier = paneItemModifier.padding(top = if (index == 0) firstTopPadding else OpalTheme.spacing.xxSmall)
        ) {
            if (index > 0) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_south),
                        contentDescription = null,
                        tint = OpalTheme.colors.contentTertiary
                    )
                }
            }
            TierCard(
                tier = tier,
                successfulReferrals = program.successfulReferrals,
                claiming = tier.id == claimingTierId,
                claimable = claimingTierId == null || tier.id == claimingTierId,
                active = tier.id == activeTierId,
                reveal = reveal,
                onClaim = { onIntent(ReferralIntent.Claim(tier.id)) }
            )
        }
    }
}

@Composable
private fun InviteCard(invitedBy: String?, modifier: Modifier = Modifier) {
    val shape = OpalTheme.shapes.large
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(194.dp)
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
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_opal_logo),
                contentDescription = stringResource(R.string.referral_opal_logo),
                tint = OpalTheme.colors.content
            )
            OpalImage(
                id = R.drawable.img_avatar_placeholder,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            if (invitedBy != null) {
                Text(
                    text = stringResource(R.string.referral_invited_by, invitedBy),
                    style = OpalTheme.typography.caption,
                    color = OpalTheme.colors.contentHigh
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
private fun Code(code: String, onCopy: () -> Unit) {
    val description = stringResource(R.string.referral_code_description, code.toList().joinToString(" "))
    val cells = remember(code.length) { mutableStateListOf(*arrayOfNulls<Rect>(code.length)) }
    val shape = OpalTheme.shapes.small
    val fill = OpalTheme.colors.fill
    val interactionSource = remember { MutableInteractionSource() }
    val indicationSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.referral_code_title),
            style = OpalTheme.typography.headline,
            modifier = Modifier.clearAndSetSemantics {}
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .pressFeedback(interactionSource, indicationSource)
                .drawWithCache {
                    val mask = Path()
                    cells.forEach { cell ->
                        if (cell != null) {
                            val outline = shape.createOutline(cell.size, layoutDirection, this)
                            mask.addPath(Path().apply { addOutline(outline) }, cell.topLeft)
                        }
                    }
                    onDrawWithContent {
                        drawPath(mask, fill)
                        clipPath(mask) { this@onDrawWithContent.drawContent() }
                    }
                }
                .indication(indicationSource, opalRipple(OpalTheme.colors.content, OpalTheme.alpha.rippleOnFill))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = stringResource(R.string.referral_copy_invite),
                    onLongClick = onCopy,
                    onClick = onCopy
                )
                .clearAndSetSemantics { contentDescription = description }
        ) {
            code.forEachIndexed { index, char ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 44.dp, height = 56.dp)
                        .onPlaced { cells[index] = it.boundsInParent() }
                ) {
                    Text(text = char.toString(), style = OpalTheme.typography.largeTitle)
                }
            }
        }
    }
}

@Composable
private fun TierCard(
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

@get:StringRes
private val MilestoneStone.title: Int
    get() = when (this) {
        MilestoneStone.Loyal -> R.string.referral_milestone_loyal
        MilestoneStone.Soulful -> R.string.referral_milestone_soulful
        MilestoneStone.Popular -> R.string.referral_milestone_popular
    }

private val Reward.isFreeYear: Boolean
    get() = this is Reward.ProAccess && term == ProTerm.OneYear

@Composable
private fun Reward.title(): String = stringResource(
    when (this) {
        is Reward.Milestone -> stone.title

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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.medium, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
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
}

private enum class Body { Blank, Loading, Error, Program }

@Composable
private fun rememberBody(target: Body): Body {
    val inspection = LocalInspectionMode.current
    var body by remember { mutableStateOf(if (target == Body.Loading && !inspection) Body.Blank else target) }
    var loadingSince by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(target) {
        when {
            target == body -> Unit

            target == Body.Loading -> {
                delay(OpalMotion.LOADING_DELAY_MILLIS)
                loadingSince = SystemClock.uptimeMillis()
                body = Body.Loading
            }

            else -> {
                if (body == Body.Loading) {
                    delay(loadingSince + OpalMotion.LOADING_MIN_MILLIS - SystemClock.uptimeMillis())
                }
                body = target
            }
        }
    }
    return body
}

@Composable
private fun Loading(headerState: ScrollState, listState: ScrollState) {
    OpalSkeleton(description = stringResource(R.string.referral_loading), modifier = Modifier.fillMaxSize()) {
        if (isTwoPane()) {
            Row(Modifier.fillMaxSize()) {
                SkeletonPane(Pane.Start, headerState, Modifier.weight(1f)) { HeaderSkeleton() }
                SkeletonPane(Pane.End, listState, Modifier.weight(1f)) { TiersSkeleton() }
            }
        } else {
            SkeletonPane(Pane.Single, listState, Modifier.fillMaxWidth()) {
                HeaderSkeleton()
                TiersSkeleton()
            }
        }
    }
}

@Composable
private fun SkeletonPane(
    pane: Pane,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge),
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(paneContentPadding(pane)),
        content = content
    )
}

@Composable
private fun HeaderSkeleton() {
    SkeletonBlock(paneItemModifier.height(194.dp), OpalTheme.shapes.large)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)
        ) {
            SkeletonLine(OpalTheme.typography.headline, 220.dp)
            SkeletonLine(OpalTheme.typography.callout, 260.dp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
        ) {
            SkeletonLine(OpalTheme.typography.headline, 140.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(SKELETON_CODE_LENGTH) { SkeletonBlock(Modifier.size(width = 44.dp, height = 56.dp)) }
            }
        }
    }
    SkeletonBlock(paneItemModifier.height(50.dp), OpalTheme.shapes.pill)
}

@Composable
private fun TiersSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxSmall),
        modifier = paneItemModifier
    ) {
        repeat(SKELETON_TIERS) { index ->
            if (index > 0) Spacer(Modifier.height(32.dp))
            TierCardSkeleton()
        }
    }
}

@Composable
private fun TierCardSkeleton() {
    OpalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBlock(Modifier.size(100.dp), OpalTheme.shapes.medium)
            Column(
                verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = OpalTheme.spacing.xxSmall)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xxxSmall)) {
                    SkeletonLine(OpalTheme.typography.caption, 64.dp)
                    Column {
                        SkeletonLine(OpalTheme.typography.body, 120.dp)
                        SkeletonLine(OpalTheme.typography.caption, 160.dp)
                    }
                }
                SkeletonBlock(
                    Modifier
                        .widthIn(max = 179.dp)
                        .fillMaxWidth()
                        .height(6.dp),
                    OpalTheme.shapes.pill
                )
            }
        }
    }
}

private const val SKELETON_CODE_LENGTH = 5
private const val SKELETON_TIERS = 3

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

@Preview(widthDp = 874, heightDp = 402)
@Composable
private fun ReferralContentLoadingLandscapePreview() {
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

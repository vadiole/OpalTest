package vadiole.opaltest.feature.referral

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import vadiole.opaltest.R
import vadiole.opaltest.container
import vadiole.opaltest.domain.referral.Plan
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.Reward
import vadiole.opaltest.feature.claimreveal.ClaimRevealHost
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.RevealContent
import vadiole.opaltest.feature.claimreveal.rememberClaimRevealState
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalGlow
import vadiole.opaltest.ui.common.OpalIconButton
import vadiole.opaltest.ui.common.OpalTopBlur
import vadiole.opaltest.ui.common.rememberTopBlurVisibility
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        viewModel.effects.flowWithLifecycle(lifecycle).collect { effect ->
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
                        val content = RevealContent(gem = reward.stone.revealImage, title = reward.stone.title)
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
                    when (state) {
                        is ReferralState.Ready -> Body.Program
                        is ReferralState.Failed -> Body.Error
                        ReferralState.Loading -> Body.Loading
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
                        Body.Program -> (state as? ReferralState.Ready)?.let { ready ->
                            Program(
                                program = ready.program,
                                claimingTierId = ready.claimingTierId,
                                reveal = reveal,
                                onIntent = onIntent,
                                twoPane = twoPane,
                                listState = listState,
                                headerState = headerState,
                                tiersState = tiersState
                            )
                        }

                        Body.Error -> LoadError(
                            retrying = state == ReferralState.Loading,
                            onRetry = { onIntent(ReferralIntent.Retry) }
                        )

                        Body.Loading -> Loading(headerState = skeletonHeaderState, listState = skeletonListState)

                        Body.Blank -> Unit
                    }
                }
            }
            OpalTopBlur(
                state = hazeState,
                visibility = {
                    when {
                        state !is ReferralState.Ready -> 0f
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
                    .semantics { traversalIndex = -1f }
                    .safeDrawingPadding()
                    .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.small)
            )
        }
    }
}

@Composable
private fun LoadError(retrying: Boolean, onRetry: () -> Unit) {
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
                .semantics { liveRegion = LiveRegionMode.Polite }
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
                size = OpalButtonSize.Small,
                loading = retrying
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

@Preview(widthDp = 402, heightDp = 1861)
@Composable
private fun ReferralContentFigmaPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState.Ready(previewProgram()),
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
            state = ReferralState.Ready(previewProgram(plan = Plan.Pro, referrals = 0)),
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
            state = ReferralState.Ready(previewProgram(), claimingTierId = "soulful"),
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
            state = ReferralState.Ready(previewProgram(referrals = 12)),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ReferralContentLoadingPreview() {
    OpalTheme {
        ReferralContent(state = ReferralState.Loading, onIntent = {}, onBack = {})
    }
}

@Preview(widthDp = 874, heightDp = 402)
@Composable
private fun ReferralContentLoadingLandscapePreview() {
    OpalTheme {
        ReferralContent(state = ReferralState.Loading, onIntent = {}, onBack = {})
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Composable
private fun ReferralContentErrorPreview() {
    OpalTheme {
        ReferralContent(
            state = ReferralState.Failed(ReferralError.Network),
            onIntent = {},
            onBack = {}
        )
    }
}

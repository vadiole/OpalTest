package vadiole.opaltest.feature.claimreveal.demo

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.MilestoneStone
import vadiole.opaltest.feature.claimreveal.ClaimRevealHost
import vadiole.opaltest.feature.claimreveal.ClaimRevealState
import vadiole.opaltest.feature.claimreveal.ClaimedGemGlow
import vadiole.opaltest.feature.claimreveal.RevealContent
import vadiole.opaltest.feature.claimreveal.RevealSound
import vadiole.opaltest.feature.claimreveal.claimRevealSource
import vadiole.opaltest.feature.claimreveal.rememberClaimRevealState
import vadiole.opaltest.feature.claimreveal.revealImage
import vadiole.opaltest.feature.claimreveal.thumbnail
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.common.OpalButtonSize
import vadiole.opaltest.ui.common.OpalButtonStyle
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalChip
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
    var last by rememberSaveable { mutableStateOf<MilestoneStone?>(null) }
    val names = demoTiers.associate { it.stone to stringResource(it.name) }
    fun open(stone: MilestoneStone) {
        last = stone
        claimed = claimed + stone
        reveal.open(stone, RevealContent(gem = stone.revealImage, title = names.getValue(stone)))
    }
    fun replay() {
        val stone = last ?: return
        if (reveal.isHolding(stone)) reveal.replay() else open(stone)
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ClaimRevealHost(
        state = reveal,
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                val sound = soundKeys[event.key]
                when {
                    sound != null -> reveal.sound = sound
                    event.key == Key.Zero -> reveal.sound = null
                    event.key == Key.S -> reveal.timeScale = if (reveal.timeScale == 1f) SLOW_MOTION else 1f
                    event.key == Key.R -> replay()
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusRequester(focus)
            .focusable()
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
                    DemoTierCard(
                        tier = tier,
                        name = names.getValue(tier.stone),
                        claimed = tier.stone in claimed && !reveal.isHolding(tier.stone),
                        reveal = reveal,
                        onClaim = { open(tier.stone) }
                    )
                }
                DemoPanel(reveal = reveal, onReplay = ::replay)
                OpalButton(
                    text = stringResource(R.string.claim_reveal_demo_reset),
                    onClick = { claimed = emptyList() },
                    style = OpalButtonStyle.Primary,
                    size = OpalButtonSize.Small
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
                ClaimedGemGlow(gem = tier.stone.thumbnail, visible = claimed, modifier = Modifier.fillMaxSize())
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
                        size = OpalButtonSize.Small
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoPanel(reveal: ClaimRevealState, onReplay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
        Text(
            text = stringResource(R.string.claim_reveal_demo_sound),
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
            OpalChip(
                text = stringResource(R.string.claim_reveal_demo_random),
                selected = reveal.sound == null,
                onClick = { reveal.sound = null }
            )
            soundKeys.values.forEach { sound ->
                OpalChip(
                    text = sound.name.takeLast(1),
                    selected = reveal.sound == sound,
                    onClick = { reveal.sound = sound }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
            OpalChip(
                text = stringResource(R.string.claim_reveal_demo_slow),
                selected = reveal.timeScale != 1f,
                onClick = { reveal.timeScale = if (reveal.timeScale == 1f) SLOW_MOTION else 1f }
            )
            OpalChip(text = stringResource(R.string.claim_reveal_demo_replay), selected = false, onClick = onReplay)
        }
        Text(
            text = stringResource(R.string.claim_reveal_demo_path, reveal.pathBend),
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentSecondary
        )
        Slider(value = reveal.pathBend, onValueChange = { reveal.pathBend = it }, valueRange = -1f..1f)
        Text(
            text = stringResource(R.string.claim_reveal_demo_hint),
            style = OpalTheme.typography.caption,
            color = OpalTheme.colors.contentSecondary
        )
    }
}

private val soundKeys = linkedMapOf(
    Key.One to RevealSound.Shine1,
    Key.Two to RevealSound.Shine2,
    Key.Three to RevealSound.Shine3,
    Key.Five to RevealSound.Shine5,
    Key.Six to RevealSound.Shine6
)

private const val SLOW_MOTION = 4f

@Preview(widthDp = 402, heightDp = 874)
@Preview(widthDp = 874, heightDp = 402)
@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ClaimRevealDemoScreenPreview() {
    OpalTheme {
        ClaimRevealDemoScreen()
    }
}

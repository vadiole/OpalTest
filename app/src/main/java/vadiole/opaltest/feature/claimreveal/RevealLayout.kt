package vadiole.opaltest.feature.claimreveal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.window.core.layout.WindowSizeClass
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalButton
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun RevealLayout(state: ClaimRevealState, title: String, continueFocus: FocusRequester) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val paneTitle = stringResource(R.string.claim_reveal_pane)
    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .semantics { this.paneTitle = paneTitle }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val minHeight = maxHeight
            val wideGem = min(MAX_GEM_SIZE, min(maxHeight * 0.78f, maxWidth * 0.4f))
            val compactGem = min(MAX_GEM_SIZE, min(maxWidth * 0.8f, maxHeight * 0.44f))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (wide) {
                    WideReveal(
                        state = state,
                        title = title,
                        continueFocus = continueFocus,
                        gemSize = wideGem,
                        modifier = Modifier.heightIn(min = minHeight)
                    )
                } else {
                    CompactReveal(
                        state = state,
                        title = title,
                        continueFocus = continueFocus,
                        gemSize = compactGem,
                        modifier = Modifier.heightIn(min = minHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactReveal(
    state: ClaimRevealState,
    title: String,
    continueFocus: FocusRequester,
    gemSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.medium)
    ) {
        Spacer(Modifier.height(OpalTheme.spacing.large))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GemSlot(state, gemSize)
            Spacer(Modifier.height(OpalTheme.spacing.large))
            RevealTexts(state = state, title = title, textAlign = TextAlign.Center)
        }
        ContinueButton(
            state = state,
            modifier = Modifier
                .padding(top = OpalTheme.spacing.xLarge)
                .widthIn(max = MAX_TEXT_WIDTH)
                .fillMaxWidth()
                .focusRequester(continueFocus)
        )
    }
}

@Composable
private fun WideReveal(
    state: ClaimRevealState,
    title: String,
    continueFocus: FocusRequester,
    gemSize: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge * 2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OpalTheme.spacing.large, vertical = OpalTheme.spacing.medium)
    ) {
        GemSlot(state, gemSize)
        Column(Modifier.widthIn(max = MAX_TEXT_WIDTH)) {
            RevealTexts(state = state, title = title, textAlign = TextAlign.Start)
            ContinueButton(
                state = state,
                modifier = Modifier
                    .padding(top = OpalTheme.spacing.xLarge)
                    .fillMaxWidth()
                    .focusRequester(continueFocus)
            )
        }
    }
}

@Composable
private fun RevealTexts(state: ClaimRevealState, title: String, textAlign: TextAlign) {
    Column(
        horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.claim_reveal_title, title),
            style = OpalTheme.typography.largeTitle.copy(lineBreak = LineBreak.Heading),
            textAlign = textAlign,
            modifier = Modifier
                .widthIn(max = MAX_TEXT_WIDTH)
                .revealStagger(state, 0)
        )
        Spacer(Modifier.height(OpalTheme.spacing.small))
        Text(
            text = stringResource(R.string.claim_reveal_message),
            style = OpalTheme.typography.callout,
            color = OpalTheme.colors.contentSecondary,
            textAlign = textAlign,
            modifier = Modifier
                .widthIn(max = MAX_TEXT_WIDTH)
                .revealStagger(state, 1)
        )
    }
}

@Composable
private fun ContinueButton(state: ClaimRevealState, modifier: Modifier = Modifier) {
    OpalButton(
        text = stringResource(R.string.claim_reveal_continue),
        onClick = { if (state.continueClickable) state.close() },
        modifier = modifier
            .revealStagger(state, 2)
    )
}

private fun Modifier.revealStagger(state: ClaimRevealState, index: Int): Modifier = graphicsLayer {
    val visible = state.items[index].value * (1f - state.back.value)
    alpha = visible
    translationY = if (state.reducedMotion) 0f else (1f - visible) * TEXT_RISE.toPx()
}

internal val ClaimRevealState.continueClickable: Boolean
    get() = items.last().value >= CLICKABLE_VISIBILITY

private const val CLICKABLE_VISIBILITY = 0.5f
private val TEXT_RISE = 12.dp
private val MAX_GEM_SIZE = 320.dp
private val MAX_TEXT_WIDTH = 400.dp

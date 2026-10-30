package vadiole.opaltest.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

private val catalogIcons = listOf(
    R.drawable.ic_arrow_back,
    R.drawable.ic_share,
    R.drawable.ic_south,
    R.drawable.ic_check_circle,
    R.drawable.ic_gift,
    R.drawable.ic_question_mark,
    R.drawable.ic_opal_logo
)

private val catalogImages = listOf(
    R.drawable.img_milestone_loyal,
    R.drawable.img_milestone_soulful,
    R.drawable.img_milestone_popular,
    R.drawable.img_reward_pro_year,
    R.drawable.img_avatar_placeholder
)

@Composable
private fun OpalCatalog(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xLarge),
            modifier = Modifier
                .widthIn(max = OpalTheme.layout.maxContentWidth)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.small)
        ) {
            OpalIconButton(icon = R.drawable.ic_arrow_back, contentDescription = "Back", onClick = onBack)
            TypographySection()
            ButtonsSection()
            ProgressSection()
            CardsSection()
            IconsSection()
            ImagesSection()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
        Text(title, style = OpalTheme.typography.caption, color = OpalTheme.colors.contentSecondary)
        content()
    }
}

@Composable
private fun TypographySection() {
    val typography = OpalTheme.typography
    Section("Typography") {
        listOf<Pair<String, TextStyle>>(
            "largeTitle 36" to typography.largeTitle,
            "headline 17 bold" to typography.headline,
            "body 17 medium" to typography.body,
            "calloutMedium 16" to typography.calloutMedium,
            "callout 16" to typography.callout,
            "caption 13" to typography.caption
        ).forEach { (name, style) -> Text(name, style = style) }
        Text("accent gradient text", style = typography.body.copy(brush = OpalTheme.colors.accent))
        Text("contentSecondary text", style = typography.caption, color = OpalTheme.colors.contentSecondary)
    }
}

@Composable
private fun ButtonsSection() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    Section("Buttons: tap to see loading") {
        LoadingButton(text = "Share invite", enabled = enabled, leadingIcon = R.drawable.ic_share)
        LoadingButton(text = "Share invite", enabled = enabled)
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
            LoadingButton(
                text = "Claim",
                enabled = enabled,
                style = OpalButtonStyle.Primary,
                size = OpalButtonSize.Small
            )
            OpalButton(
                text = if (enabled) "Disable all" else "Enable all",
                onClick = { enabled = !enabled },
                style = OpalButtonStyle.Primary,
                size = OpalButtonSize.Small
            )
        }
    }
}

@Composable
private fun LoadingButton(
    text: String,
    enabled: Boolean,
    style: OpalButtonStyle = OpalButtonStyle.Accent,
    size: OpalButtonSize = OpalButtonSize.Large,
    @DrawableRes leadingIcon: Int? = null
) {
    var loading by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(1500)
            loading = false
        }
    }
    OpalButton(
        text = text,
        onClick = { loading = true },
        style = style,
        size = size,
        leadingIcon = leadingIcon,
        enabled = enabled,
        loading = loading,
        modifier = if (size == OpalButtonSize.Large) Modifier.fillMaxWidth() else Modifier
    )
}

@Composable
private fun ProgressSection() {
    var progress by rememberSaveable { mutableFloatStateOf(0.3f) }
    Section("Progress bar: ${(progress * 100).toInt()}%") {
        OpalProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        OpalProgressBar(progress = progress, modifier = Modifier.size(width = 179.dp, height = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
            OpalButton(
                text = "−20%",
                onClick = { progress = (progress - 0.2f).coerceAtLeast(0f) },
                style = OpalButtonStyle.Primary,
                size = OpalButtonSize.Small
            )
            OpalButton(
                text = "+20%",
                onClick = { progress = (progress + 0.2f).coerceAtMost(1f) },
                style = OpalButtonStyle.Primary,
                size = OpalButtonSize.Small
            )
        }
    }
}

@Composable
private fun CardsSection() {
    Section("Cards") {
        OpalCard(Modifier.fillMaxWidth()) { Text("OpalCard", style = OpalTheme.typography.body) }
        OpalCard(Modifier.fillMaxWidth(), highlighted = true) {
            Text("OpalCard highlighted", style = OpalTheme.typography.body)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconsSection() {
    Section("Icons at their natural size") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            catalogIcons.forEach { Icon(painter = painterResource(it), contentDescription = null) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImagesSection() {
    Section("Images") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)
        ) {
            catalogImages.forEach {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(OpalTheme.shapes.medium)
                        .background(OpalTheme.colors.fillSubtle)
                        .size(100.dp)
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.img_invite_card_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(362f / 194f)
                .clip(OpalTheme.shapes.large)
        )
    }
}

@Preview(widthDp = 402, heightDp = 2000)
@Composable
private fun OpalCatalogPreview() {
    OpalTheme {
        OpalCatalog(onBack = {})
    }
}

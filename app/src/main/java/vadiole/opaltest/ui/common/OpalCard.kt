package vadiole.opaltest.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalCard(modifier: Modifier = Modifier, highlighted: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val shape = OpalTheme.shapes.xLarge
    val fill = OpalTheme.colors.fillSubtle
    val highlight = animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = OpalMotion.StateSpring,
        label = "cardHighlight"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind { if (highlight.value > 0f) drawRect(fill, alpha = highlight.value) }
            .outlineBorder(OpalTheme.layout.borderWidth, OpalTheme.colors.border, shape)
            .padding(OpalTheme.spacing.small),
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OpalCardPreview() {
    OpalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small),
            modifier = Modifier.padding(OpalTheme.spacing.mediumLarge)
        ) {
            OpalCard(Modifier.fillMaxWidth()) { Text("Card", style = OpalTheme.typography.body) }
            OpalCard(Modifier.fillMaxWidth(), highlighted = true) {
                Text("Highlighted card", style = OpalTheme.typography.body)
            }
        }
    }
}

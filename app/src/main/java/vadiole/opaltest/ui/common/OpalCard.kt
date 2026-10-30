package vadiole.opaltest.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun OpalCard(modifier: Modifier = Modifier, highlighted: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val shape = OpalTheme.shapes.xLarge
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (highlighted) OpalTheme.colors.fillSubtle else Color.Transparent)
            .outlineBorder(1.dp, OpalTheme.colors.border, shape)
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

package vadiole.opaltest.feature.console

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ConsoleScreen(onOpenReferrals: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.safeDrawingPadding()) {
            Button(onClick = onOpenReferrals) {
                Text(stringResource(R.string.console_open_referrals))
            }
        }
    }
}

@Preview
@Composable
private fun ConsoleScreenPreview() {
    OpalTheme {
        ConsoleScreen(onOpenReferrals = {})
    }
}

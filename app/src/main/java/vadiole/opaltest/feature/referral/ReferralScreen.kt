package vadiole.opaltest.feature.referral

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
fun ReferralScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.safeDrawingPadding()) {
            Text(stringResource(R.string.referral_title))
        }
    }
}

@Preview
@Composable
private fun ReferralScreenPreview() {
    OpalTheme {
        ReferralScreen()
    }
}

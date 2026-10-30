package vadiole.opaltest.feature.referral

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalIconButton
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
fun ReferralScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalTheme.colors.background)
            .safeDrawingPadding()
            .padding(horizontal = OpalTheme.spacing.mediumLarge, vertical = OpalTheme.spacing.small)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            OpalIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.referral_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(text = stringResource(R.string.referral_title), style = OpalTheme.typography.headline)
        }
    }
}

@Preview
@Composable
private fun ReferralScreenPreview() {
    OpalTheme {
        ReferralScreen(onBack = {})
    }
}

package vadiole.opaltest.feature.claimreveal.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vadiole.opaltest.ui.theme.OpalTheme

class ClaimRevealDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OpalTheme {
                ClaimRevealDemoScreen()
            }
        }
    }
}

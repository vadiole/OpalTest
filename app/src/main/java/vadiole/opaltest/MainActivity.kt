package vadiole.opaltest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vadiole.opaltest.navigation.AppNavigation
import vadiole.opaltest.ui.theme.OpalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OpalTheme {
                AppNavigation()
            }
        }
    }
}

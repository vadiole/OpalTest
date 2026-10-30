package vadiole.opaltest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vadiole.opaltest.navigation.AppNavigation
import vadiole.opaltest.ui.theme.OpalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            // auto() keeps nav bar contrast enforced, so the system scrim shows only in 3-button mode:
            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/com/android/internal/policy/DecorView.java;l=1409
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT, detectDarkMode = { true })
        )
        super.onCreate(savedInstanceState)
        setContent {
            OpalTheme {
                AppNavigation()
            }
        }
    }
}

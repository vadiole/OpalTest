package vadiole.opaltest.ui.common

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.delay
import vadiole.opaltest.ui.theme.OpalMotion

@Composable
fun <P : Any> rememberPacedPhase(target: P, loading: P): P? {
    val inspection = LocalInspectionMode.current
    var shown by remember { mutableStateOf(if (target == loading && !inspection) null else target) }
    var loadingSince by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(target) {
        when {
            target == shown -> Unit

            target == loading -> {
                delay(OpalMotion.LOADING_DELAY_MILLIS)
                loadingSince = SystemClock.uptimeMillis()
                shown = loading
            }

            else -> {
                if (shown == loading) {
                    delay(loadingSince + OpalMotion.LOADING_MIN_MILLIS - SystemClock.uptimeMillis())
                }
                shown = target
            }
        }
    }
    return shown
}

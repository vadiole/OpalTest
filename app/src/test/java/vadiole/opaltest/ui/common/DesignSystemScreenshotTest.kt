package vadiole.opaltest.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vadiole.opaltest.R
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w402dp-h874dp-xxhdpi")
class DesignSystemScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun components() = capture("ds_components") { Components() }

    @Test
    fun componentsFontScale2() = capture("ds_components_font2", fontScale = 2f) { Components() }

    @Test
    fun longLabels() = capture("ds_long_labels") { LongLabels() }

    @Test
    fun longLabelsFontScale2() = capture("ds_long_labels_font2", fontScale = 2f) { LongLabels() }

    @Test
    fun chipKeyboardFocus() {
        var inputModeManager: InputModeManager? = null
        compose.mainClock.autoAdvance = false
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            Themed(1f) {
                Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                    OpalChip(text = "Free", selected = true, onClick = {})
                    OpalChip(text = "Pro", selected = false, onClick = {})
                    OpalToggleChip(text = "Fail", checked = false, onCheckedChange = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnUiThread { inputModeManager?.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Pro").requestFocus().assertIsFocused()
        compose.mainClock.advanceTimeBy(500)
        val proFocused = shot()
        compose.onNodeWithText("Free").requestFocus().assertIsFocused()
        compose.mainClock.advanceTimeBy(500)
        write("ds_chip_focus", stack(listOf(proFocused, shot())))
    }

    @Test
    fun loadingAndEnabledTransitions() {
        var busy by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Themed(1f) {
                Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                    OpalButton(text = "Share invite", onClick = {}, loading = busy, leadingIcon = R.drawable.ic_share)
                    OpalButton(
                        text = "Claim",
                        onClick = {},
                        style = OpalButtonStyle.Neutral,
                        size = OpalButtonSize.Small,
                        enabled = !busy
                    )
                    OpalChip(text = "Pro", selected = false, enabled = !busy, onClick = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val frames = mutableListOf(shot())
        busy = true
        for (step in listOf(16L, 32L, 48L, 80L, 600L)) {
            compose.mainClock.advanceTimeBy(step)
            frames += shot()
        }
        busy = false
        for (step in listOf(48L, 600L)) {
            compose.mainClock.advanceTimeBy(step)
            frames += shot()
        }
        write("ds_transitions", stack(frames))
    }

    @Composable
    private fun Components() {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
            OpalButton(
                text = "Share invite",
                onClick = {},
                leadingIcon = R.drawable.ic_share,
                modifier = Modifier.fillMaxWidth()
            )
            OpalButton(text = "Share invite", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
            OpalButton(text = "Next reward", onClick = {}, style = OpalButtonStyle.Neutral)
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                OpalButton(text = "Claim", onClick = {}, style = OpalButtonStyle.Neutral, size = OpalButtonSize.Small)
                OpalButton(
                    text = "Claim",
                    onClick = {},
                    style = OpalButtonStyle.Neutral,
                    size = OpalButtonSize.Small,
                    loading = true
                )
                OpalButton(
                    text = "Claim",
                    onClick = {},
                    style = OpalButtonStyle.Neutral,
                    size = OpalButtonSize.Small,
                    enabled = false
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                OpalButton(text = "+1", onClick = {}, style = OpalButtonStyle.Outlined, size = OpalButtonSize.Small)
                OpalButton(
                    text = "Reset",
                    onClick = {},
                    style = OpalButtonStyle.Outlined,
                    size = OpalButtonSize.Small,
                    loading = true
                )
                OpalButton(
                    text = "All unlocked",
                    onClick = {},
                    style = OpalButtonStyle.Outlined,
                    size = OpalButtonSize.Small,
                    enabled = false
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                OpalChip(text = "Free", selected = true, onClick = {})
                OpalChip(text = "Pro", selected = false, onClick = {})
                OpalChip(text = "Off", selected = false, enabled = false, onClick = {})
                OpalToggleChip(text = "Fail", checked = true, onCheckedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                OpalIconButton(icon = R.drawable.ic_arrow_back, contentDescription = null, onClick = {})
                OpalIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = null,
                    onClick = {},
                    enabled = false
                )
            }
        }
    }

    @Composable
    private fun LongLabels() {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
            OpalButton(text = "Open Invitations & Rewards", onClick = {}, modifier = Modifier.fillMaxWidth())
            OpalButton(
                text = "Zum Zurücksetzen erneut tippen",
                onClick = {},
                style = OpalButtonStyle.Neutral,
                modifier = Modifier.width(220.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.small)) {
                OpalButton(
                    text = "Nächste Belohnung",
                    onClick = {},
                    style = OpalButtonStyle.Neutral,
                    modifier = Modifier.weight(1f)
                )
                OpalButton(
                    text = "Demo zurücksetzen",
                    onClick = {},
                    style = OpalButtonStyle.Outlined,
                    size = OpalButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
                OpalButton(
                    text = "Jetzt einlösen",
                    onClick = {},
                    style = OpalButtonStyle.Neutral,
                    size = OpalButtonSize.Small,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                OpalChip(text = "Verzögerung lang", selected = true, onClick = {}, modifier = Modifier.width(120.dp))
                OpalToggleChip(
                    text = "Nächstes Laden fehlschlagen",
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }

    @Composable
    private fun Themed(fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalInspectionMode provides true,
            LocalDensity provides Density(density.density, fontScale)
        ) {
            OpalTheme {
                Column(
                    Modifier
                        .background(OpalTheme.colors.background)
                        .padding(OpalTheme.spacing.mediumLarge)
                ) { content() }
            }
        }
    }

    private fun capture(name: String, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { Themed(fontScale, content) }
        compose.mainClock.advanceTimeBy(2_000)
        write(name, shot())
    }

    private fun shot(): Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()

    private fun stack(frames: List<Bitmap>): Bitmap {
        val first = frames.first()
        return Bitmap.createBitmap(first.width, first.height * frames.size, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            frames.forEachIndexed { index, frame ->
                canvas.drawBitmap(frame, 0f, (first.height * index).toFloat(), null)
            }
        }
    }

    private fun write(name: String, bitmap: Bitmap) {
        val output = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

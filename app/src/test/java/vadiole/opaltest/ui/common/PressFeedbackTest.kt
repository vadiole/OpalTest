package vadiole.opaltest.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PressFeedbackTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private val indications = mutableListOf<Interaction>()

    private fun setContent() {
        compose.setContent {
            val interactionSource = remember { MutableInteractionSource() }
            val indicationSource = remember { MutableInteractionSource() }
            LaunchedEffect(Unit) { indicationSource.interactions.collect { indications += it } }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .pressFeedback(interactionSource, indicationSource)
                        .testTag(TAG)
                        .size(100.dp)
                        .clickable(interactionSource = interactionSource, indication = null) { clicks++ }
                )
                Box(Modifier.height(2000.dp))
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun width() = compose.onNodeWithTag(TAG).getBoundsInRoot().let { it.right - it.left }.value

    private fun advance(millis: Long) {
        repeat((millis / 16).toInt()) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
    }

    @Test
    fun `press starts on down inside a scrollable container`() {
        setContent()
        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        assertTrue(indications.single() is PressInteraction.Press)
        advance(48)
        assertTrue(width() < 100f)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        advance(500)
        assertEquals(100f, width(), 0.01f)
        assertEquals(1, clicks)
        assertTrue(indications.last() is PressInteraction.Release)
    }

    @Test
    fun `tap within one frame dips and returns`() {
        setContent()
        compose.onNodeWithTag(TAG).performTouchInput {
            down(center)
            up()
        }
        advance(64)
        assertTrue(width() < 99f)
        advance(500)
        assertEquals(100f, width(), 0.01f)
        assertEquals(1, clicks)
    }

    @Test
    fun `short hold dips and returns`() {
        setContent()
        compose.onNodeWithTag(TAG).performTouchInput { down(center) }
        advance(32)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        advance(500)
        assertEquals(100f, width(), 0.01f)
    }

    @Test
    fun `dragging past touch slop cancels the press`() {
        setContent()
        compose.onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 3))
        }
        advance(500)
        assertEquals(100f, width(), 0.01f)
        assertTrue(indications.last() is PressInteraction.Cancel)
        compose.onNodeWithTag(TAG).performTouchInput { up() }
        assertEquals(0, clicks)
    }

    private companion object {
        const val TAG = "pressable"
    }
}

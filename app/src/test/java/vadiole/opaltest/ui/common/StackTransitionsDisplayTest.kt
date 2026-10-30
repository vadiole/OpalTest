package vadiole.opaltest.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vadiole.opaltest.ui.theme.OpalMotion

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StackTransitionsDisplayTest {
    @get:Rule
    val compose = createComposeRule()

    private val backStack = mutableStateListOf("console")

    private fun setContent() {
        compose.setContent {
            val transitions = rememberStackTransitions(backStack, StackMotionState(emphasized = LinearEasing))
            NavDisplay(
                backStack = backStack,
                onBack = {
                    transitions.onBackCompleted()
                    backStack.removeLastOrNull()
                },
                entryDecorators = listOf(transitions.decorator),
                transitionSpec = { transitions.hold() },
                popTransitionSpec = { transitions.hold() },
                predictivePopTransitionSpec = { transitions.hold() },
                entryProvider = { key ->
                    NavEntry(key) { Box(Modifier.fillMaxSize()) { Text(it) } }
                }
            )
        }
        compose.waitForIdle()
    }

    private fun advanceFrames(millis: Long) {
        repeat((millis / 16).toInt()) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
    }

    @Test
    fun `bottom screen stays composed for the whole push`() {
        setContent()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { backStack.add("referral") }
        advanceFrames((OpalMotion.SCREEN_DURATION * 0.8f).toLong())
        compose.onNodeWithText("console").assertExists()
        compose.onNodeWithText("referral").assertExists()
        advanceFrames(OpalMotion.SCREEN_DURATION + 500L)
        compose.onNodeWithText("console").assertDoesNotExist()
    }

    @Test
    fun `leaving screen stays composed for the whole pop`() {
        backStack.add("referral")
        setContent()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { backStack.removeLastOrNull() }
        advanceFrames((OpalMotion.SCREEN_DURATION * 0.8f).toLong())
        compose.onNodeWithText("referral").assertExists()
        advanceFrames(OpalMotion.SCREEN_DURATION + 500L)
        compose.onNodeWithText("referral").assertDoesNotExist()
    }
}

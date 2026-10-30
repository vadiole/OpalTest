package vadiole.opaltest.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val motion = StackMotionState(emphasized = LinearEasing)
    private val input = DirectNavigationEventInput()
    private var overlayProgress = -1f

    private fun setContent(overlayBackHandler: Boolean = false) {
        compose.setContent {
            val dispatcher = LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher
            DisposableEffect(dispatcher) {
                dispatcher.addInput(input)
                onDispose { dispatcher.removeInput(input) }
            }
            val transitions = rememberStackTransitions(backStack, motion)
            StackNavDisplay(
                backStack = backStack,
                transitions = transitions,
                onBack = {
                    transitions.onBackCompleted()
                    backStack.removeLastOrNull()
                },
                entryProvider = { key ->
                    NavEntry(key) {
                        Box(Modifier.fillMaxSize()) {
                            Text(it)
                            if (overlayBackHandler && it == "referral") {
                                PredictiveBackHandler { events -> events.collect { e -> overlayProgress = e.progress } }
                            }
                        }
                    }
                }
            )
        }
        compose.waitForIdle()
    }

    private fun swipeBack(progress: Float) {
        compose.runOnUiThread {
            input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0f, touchY = 100f))
            input.backProgressed(
                NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = progress, touchY = 100f)
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

    @Test
    fun `back gesture handled inside the top screen leaves the stack still`() {
        backStack.add("referral")
        setContent(overlayBackHandler = true)
        swipeBack(0.6f)
        assertEquals(0.6f, overlayProgress, 0.001f)
        assertEquals(BackPhase.Idle, motion.phase)
        assertEquals(1f, motion.pose("referral", Size(1000f, 2000f), 0f).scale)
        compose.runOnUiThread { input.backCompleted() }
        compose.waitForIdle()
        assertEquals(listOf("console", "referral"), backStack.toList())
        assertEquals(BackPhase.Idle, motion.phase)
    }

    @Test
    fun `predictive back between screens drives the stack pose and pops`() {
        backStack.add("referral")
        setContent()
        swipeBack(0.6f)
        assertEquals(BackPhase.Gesture, motion.phase)
        assertTrue(motion.pose("referral", Size(1000f, 2000f), 0f).scale < 1f)
        compose.runOnUiThread { input.backCompleted() }
        compose.waitForIdle()
        assertEquals(listOf("console"), backStack.toList())
    }
}

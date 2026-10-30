package vadiole.opaltest.ui.common

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Size
import androidx.navigationevent.NavigationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StackMotionStateTest {
    private val root = listOf("console")
    private val stack = listOf("console", "referral")
    private val size = Size(1000f, 2000f)
    private val offset = 96f
    private val event = NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.5f, touchY = 100f)
    private val startEvent = NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0f, touchY = 100f)

    private fun TestScope.frameScope(): CoroutineScope {
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                delay(16)
                return onFrame(testScheduler.currentTime * 1_000_000)
            }
        }
        return CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]) + clock)
    }

    private fun StackMotionState.pose(key: Any) = pose(key, size, 0f)

    private fun TestScope.pushed(): Pair<StackMotionState, CoroutineScope> {
        val scope = frameScope()
        val motion = StackMotionState()
        motion.onStack(root, scope)
        motion.onStack(stack, scope)
        return motion to scope
    }

    @Test
    fun `push slides the new screen in and settles`() = runTest {
        val (motion, _) = pushed()
        assertEquals(StackMotion.Push, motion.motion)
        assertEquals(offset, motion.pose("referral").translationX, 0.01f)
        advanceTimeBy(1_000)
        assertEquals(StackMotion.None, motion.motion)
        assertEquals(0f, motion.pose("referral").translationX, 0.01f)
    }

    @Test
    fun `back gesture during push ends the push and jumps to the gesture pose`() = runTest {
        val (motion, _) = pushed()
        advanceTimeBy(100)
        motion.onGesture(startEvent, stack)
        assertEquals(StackMotion.None, motion.motion)
        assertEquals(BackPhase.Gesture, motion.phase)
        val top = motion.pose("referral")
        assertEquals(0f, top.translationX, 0.01f)
        assertEquals(1f, top.alpha, 0.01f)
        assertEquals(-offset, motion.pose("console").translationX, 0.01f)
        advanceTimeBy(1_000)
        assertEquals(0f, motion.pose("referral").translationX, 0.01f)
    }

    @Test
    fun `button pop animates and keeps the popped screen hidden`() = runTest {
        val (motion, scope) = pushed()
        advanceTimeBy(1_000)
        motion.onStack(root, scope)
        assertEquals(StackMotion.Pop, motion.motion)
        advanceTimeBy(1_000)
        assertEquals(StackMotion.None, motion.motion)
        assertEquals(0f, motion.pose("referral").alpha, 0.01f)
    }

    @Test
    fun `completed gesture commits without a pop animation`() = runTest {
        val (motion, scope) = pushed()
        advanceTimeBy(1_000)
        motion.onGesture(event, stack)
        motion.onBackCompleted()
        motion.onStack(root, scope)
        assertEquals(StackMotion.None, motion.motion)
        motion.onRelease(scope)
        assertEquals(BackPhase.Commit, motion.phase)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Idle, motion.phase)
    }

    @Test
    fun `push during commit blends from the leaving pose`() = runTest {
        val (motion, scope) = pushed()
        advanceTimeBy(1_000)
        motion.onGesture(event, stack)
        motion.onBackCompleted()
        motion.onStack(root, scope)
        motion.onRelease(scope)
        val leaving = motion.pose("referral")
        motion.onStack(stack, scope)
        assertEquals(StackMotion.Blend, motion.motion)
        assertEquals(BackPhase.Idle, motion.phase)
        val start = motion.pose("referral")
        assertEquals(leaving.scale, start.scale, 0.01f)
        assertEquals(leaving.translationX, start.translationX, 0.01f)
        assertTrue(start.scale < 1f)
        advanceTimeBy(1_000)
        assertEquals(1f, motion.pose("referral").scale, 0.01f)
        assertEquals(1f, motion.pose("referral").alpha, 0.01f)
    }

    @Test
    fun `released gesture without completion cancels`() = runTest {
        val (motion, scope) = pushed()
        motion.onGesture(event, stack)
        motion.onRelease(scope)
        assertEquals(BackPhase.Cancel, motion.phase)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Idle, motion.phase)
    }

    @Test
    fun `cancelled settle returns to idle`() = runTest {
        val motion = StackMotionState()
        val scope = frameScope()
        motion.onGesture(event, stack)
        motion.onRelease(scope)
        runCurrent()
        scope.cancel()
        runCurrent()
        assertEquals(BackPhase.Idle, motion.phase)
    }

    @Test
    fun `gesture started before settle finishes takes over`() = runTest {
        val motion = StackMotionState()
        motion.onGesture(event, stack)
        motion.onRelease(frameScope())
        runCurrent()
        motion.onGesture(event, stack)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Gesture, motion.phase)
        assertTrue(motion.pose("referral").scale < 1f)
    }
}

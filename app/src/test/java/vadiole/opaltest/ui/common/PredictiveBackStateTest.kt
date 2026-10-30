package vadiole.opaltest.ui.common

import androidx.compose.runtime.MonotonicFrameClock
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PredictiveBackStateTest {
    private val stack = listOf("console", "referral")
    private val event = NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.5f, touchY = 100f)

    private fun TestScope.settleScope(): CoroutineScope {
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                delay(16)
                return onFrame(testScheduler.currentTime * 1_000_000)
            }
        }
        return CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]) + clock)
    }

    private fun state() = PredictiveBackState()

    @Test
    fun `roles come from the back stack, not the running transition`() {
        val back = state()
        back.onGesture(event, stack)
        assertEquals(BackRole.Top, back.role("referral"))
        assertEquals(BackRole.Previous, back.role("console"))
        assertEquals(BackRole.None, back.role("other"))
    }

    @Test
    fun `completed gesture commits and settles to idle`() = runTest {
        val back = state()
        back.onGesture(event, stack)
        back.onBackCompleted()
        back.onRelease(settleScope(), stack.size - 1)
        assertEquals(BackPhase.Commit, back.phase)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Idle, back.phase)
    }

    @Test
    fun `released gesture without completion cancels`() = runTest {
        val back = state()
        back.onGesture(event, stack)
        back.onRelease(settleScope(), stack.size)
        assertEquals(BackPhase.Cancel, back.phase)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Idle, back.phase)
    }

    @Test
    fun `cancelled settle returns to idle`() = runTest {
        val back = state()
        val scope = settleScope()
        back.onGesture(event, stack)
        back.onRelease(scope, stack.size)
        runCurrent()
        scope.cancel()
        runCurrent()
        assertEquals(BackPhase.Idle, back.phase)
    }

    @Test
    fun `gesture started before settle finishes takes over`() = runTest {
        val back = state()
        back.onGesture(event, stack)
        back.onRelease(settleScope(), stack.size)
        runCurrent()
        back.onGesture(event, stack)
        advanceTimeBy(1_000)
        assertEquals(BackPhase.Gesture, back.phase)
        assertEquals(BackRole.Top, back.role("referral"))
    }
}

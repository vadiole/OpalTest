package vadiole.opaltest.ui.common

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import vadiole.opaltest.ui.theme.OpalMotion.LOADING_DELAY_MILLIS
import vadiole.opaltest.ui.theme.OpalMotion.LOADING_MIN_MILLIS

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PacedPhaseTest {
    @get:Rule
    val compose = createComposeRule()

    private enum class Phase { Loading, Failed, Ready }

    private var target by mutableStateOf(Phase.Loading)
    private val history = mutableListOf<Phase?>()
    private val shown get() = history.last()

    private fun setUp(initial: Phase, inspection: Boolean = false) {
        target = initial
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides inspection) {
                val phase = rememberPacedPhase(target, loading = Phase.Loading)
                SideEffect { history += phase }
            }
        }
    }

    private fun moveTo(phase: Phase) {
        target = phase
        Snapshot.sendApplyNotifications()
    }

    private fun advance(millis: Long) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, FRAME_MILLIS)
            ShadowSystemClock.advanceBy(Duration.ofMillis(step))
            compose.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            left -= step
        }
    }

    @Test
    fun `fast load skips the loading phase`() {
        setUp(Phase.Loading)
        advance(LOADING_DELAY_MILLIS / 2)
        assertEquals(null, shown)

        moveTo(Phase.Ready)
        advance(LOADING_DELAY_MILLIS * 2)
        assertEquals(Phase.Ready, shown)
        assertFalse(Phase.Loading in history)
    }

    @Test
    fun `slow load shows loading after the delay and keeps it for the minimum time`() {
        setUp(Phase.Loading)
        advance(LOADING_DELAY_MILLIS + 50)
        assertEquals(Phase.Loading, shown)

        moveTo(Phase.Ready)
        advance(LOADING_MIN_MILLIS - 100)
        assertEquals(Phase.Loading, shown)

        advance(LOADING_MIN_MILLIS)
        assertEquals(Phase.Ready, shown)
    }

    @Test
    fun `minimum loading time counts from when loading appeared`() {
        setUp(Phase.Loading)
        advance(LOADING_DELAY_MILLIS + 50)
        assertEquals(Phase.Loading, shown)

        advance(LOADING_MIN_MILLIS - 150)
        moveTo(Phase.Ready)
        advance(50)
        assertEquals(Phase.Loading, shown)

        advance(100)
        assertEquals(Phase.Ready, shown)
    }

    @Test
    fun `retry keeps the previous phase until the delay passes`() {
        setUp(Phase.Failed)
        advance(1_000)

        moveTo(Phase.Loading)
        advance(LOADING_DELAY_MILLIS - 50)
        assertEquals(Phase.Failed, shown)

        advance(100)
        assertEquals(Phase.Loading, shown)
    }

    @Test
    fun `inspection mode shows loading right away`() {
        setUp(Phase.Loading, inspection = true)
        assertEquals(Phase.Loading, history.first())
    }

    private companion object {
        const val FRAME_MILLIS = 16L
    }
}

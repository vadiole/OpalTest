package vadiole.opaltest.feature.claimreveal

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimRevealStateTest {
    private val scope = TestScope()
    private val saver = ClaimRevealState.saver(scope)
    private val saverScope = SaverScope { true }

    @Test
    fun `shine sound plays at its own moment regardless of the sweep length`() {
        RevealSound.entries.forEach { sound ->
            assertEquals((1400 - sound.leadMs).toLong(), shineSoundDelayMs(sound, 1f))
            assertEquals((2800 - sound.leadMs).toLong(), shineSoundDelayMs(sound, 2f))
        }
        assertEquals(0L, shineSoundDelayMs(RevealSound.Shine1, 0.1f))
    }

    @Test
    fun `a revealed state keeps its particle clock and settled sparkle through the saver`() {
        val saved = listOf(RevealSound.Shine2.ordinal, 1.5f, "gem", GEM, TITLE, 7.25f)
        val restored = restore(saved)
        assertEquals(RevealPhase.Revealed, restored.phase)
        assertEquals(RevealContent(GEM, TITLE), restored.active?.content)
        assertEquals(7.25f, restored.sparkleTime, 0f)
        assertEquals(1f, restored.sparkle.value, 0f)
        assertEquals(1f, restored.sweep.value, 0f)
        assertEquals(saved, with(saver) { saverScope.save(restored) })
    }

    @Test
    fun `an idle state saves no particle clock`() {
        val idle = ClaimRevealState(scope)
        val saved = with(saver) { saverScope.save(idle) }!!
        assertNull(saved[5])
        val restored = restore(saved)
        assertNull(restored.active)
        assertEquals(0f, restored.sparkleTime, 0f)
        assertEquals(0f, restored.sparkle.value, 0f)
    }

    @Test
    fun `a reveal restored with reduced motion shows no motes`() {
        val reduced = TestScope(NoMotion)
        val restored = ClaimRevealState.saver(reduced)
            .restore(listOf(null, 1f, "gem", GEM, TITLE, 7.25f))!!
            .also { reduced.runCurrent() }
        assertEquals(RevealPhase.Revealed, restored.phase)
        assertTrue(restored.reducedMotion)
        assertEquals(0f, restored.sparkle.value, 0f)
        assertEquals(0f, restored.sparkleTime, 0f)
    }

    private fun restore(saved: List<Any?>): ClaimRevealState = saver.restore(saved)!!.also { scope.runCurrent() }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor = 0f
    }

    private companion object {
        const val GEM = 1
        const val TITLE = 2
    }
}

package vadiole.opaltest.feature.claimreveal

import org.junit.Assert.assertEquals
import org.junit.Test

class ClaimRevealStateTest {
    @Test
    fun `shine sound plays at its own moment regardless of the sweep length`() {
        RevealSound.entries.forEach { sound ->
            assertEquals((1400 - sound.leadMs).toLong(), shineSoundDelayMs(sound, 1f))
            assertEquals((2800 - sound.leadMs).toLong(), shineSoundDelayMs(sound, 2f))
        }
        assertEquals(0L, shineSoundDelayMs(RevealSound.Shine1, 0.1f))
    }
}

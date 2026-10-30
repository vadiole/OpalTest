package vadiole.opaltest.feature.claimreveal

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShuffleBagTest {
    @Test
    fun `every round plays each sound once and no sound repeats back to back`() {
        repeat(SEEDS) { seed ->
            val bag = ShuffleBag(RevealSound.entries, Random(seed))
            val draws = List(ROUNDS * RevealSound.entries.size) { bag.next() }
            draws.chunked(RevealSound.entries.size).forEach { round ->
                assertEquals(RevealSound.entries.toSet(), round.toSet())
            }
            draws.zipWithNext().forEach { (previous, next) -> assertNotEquals(previous, next) }
        }
    }

    @Test
    fun `a single item bag keeps returning it`() {
        val bag = ShuffleBag(listOf(RevealSound.Shine1), Random(0))
        repeat(3) { assertEquals(RevealSound.Shine1, bag.next()) }
    }

    private companion object {
        const val SEEDS = 200
        const val ROUNDS = 50
    }
}

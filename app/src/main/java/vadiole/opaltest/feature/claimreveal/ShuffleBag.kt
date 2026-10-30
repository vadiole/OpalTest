package vadiole.opaltest.feature.claimreveal

import kotlin.random.Random

internal class ShuffleBag<T>(private val items: List<T>, private val random: Random = Random.Default) {
    private val pending = ArrayDeque<T>()
    private var last: T? = null

    fun next(): T {
        if (pending.isEmpty()) refill()
        return pending.removeFirst().also { last = it }
    }

    private fun refill() {
        val round = items.shuffled(random).toMutableList()
        if (round.size > 1 && round.first() == last) {
            val swap = random.nextInt(1, round.size)
            round[0] = round[swap].also { round[swap] = round[0] }
        }
        pending.addAll(round)
    }
}

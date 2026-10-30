package vadiole.opaltest.feature.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vadiole.opaltest.data.fake.ReferralDebugControls

class ConsoleViewModel(private val controls: ReferralDebugControls) : ViewModel() {
    val state: StateFlow<ConsoleState> = controls.state
        .map { ConsoleState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsoleState())

    private val effectChannel = Channel<ConsoleEffect>(Channel.BUFFERED)
    val effects: Flow<ConsoleEffect> = effectChannel.receiveAsFlow()

    private val mutex = Mutex()

    fun onIntent(intent: ConsoleIntent) {
        if (intent == ConsoleIntent.OpenReferrals) {
            effectChannel.trySend(ConsoleEffect.OpenReferrals)
            return
        }
        viewModelScope.launch {
            mutex.withLock { handle(intent) }
        }
    }

    private suspend fun handle(intent: ConsoleIntent) {
        val debug = controls.state.first()
        when (intent) {
            ConsoleIntent.AddFriend -> {
                val count = debug.successfulReferrals + 1
                controls.setReferrals(count)
                val reached = debug.nextTierAt?.let { count >= it } == true
                effectChannel.send(if (reached) ConsoleEffect.TierReached else ConsoleEffect.FriendAdded)
            }

            ConsoleIntent.AdvanceToNextTier -> {
                if (debug.nextTierAt == null) return
                controls.advanceToNextTier()
                effectChannel.send(ConsoleEffect.TierReached)
            }

            is ConsoleIntent.SetPlan -> controls.setPlan(intent.plan)

            is ConsoleIntent.SetLatency -> controls.setLatency(intent.latency)

            ConsoleIntent.ToggleFailNextLoad -> controls.setFailNextLoad(!debug.failNextLoad)

            ConsoleIntent.Reset -> controls.reset()

            ConsoleIntent.OpenReferrals -> Unit
        }
    }
}

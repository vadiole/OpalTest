package vadiole.opaltest.feature.console

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vadiole.opaltest.data.debug.ReferralDebugControls

class ConsoleViewModel(private val controls: ReferralDebugControls) : ViewModel() {
    private val mutableState = MutableStateFlow(ConsoleState())
    val state: StateFlow<ConsoleState> = mutableState.asStateFlow()

    private val effectChannel = Channel<ConsoleEffect>(Channel.BUFFERED)
    val effects: Flow<ConsoleEffect> = effectChannel.receiveAsFlow()

    private val mutex = Mutex()
    private var loadJob: Job? = null
    private var resetTimeoutJob: Job? = null

    init {
        load()
    }

    fun onIntent(intent: ConsoleIntent) {
        val previous = state.value.load
        mutableState.update { reduce(it, intent) }
        when (intent) {
            ConsoleIntent.Retry -> load()

            ConsoleIntent.OpenReferrals -> effectChannel.trySend(ConsoleEffect.OpenReferrals)

            is ConsoleIntent.Reset -> {
                if (previous !is ConsoleLoad.Ready) return
                resetTimeoutJob?.cancel()
                if (previous.isConfirmingReset) {
                    save(intent)
                } else {
                    resetTimeoutJob = viewModelScope.launch {
                        delay(intent.confirmTimeout)
                        onIntent(ConsoleIntent.CancelReset)
                    }
                }
            }

            ConsoleIntent.CancelReset, ConsoleIntent.LoadFailed, is ConsoleIntent.Loaded -> Unit

            ConsoleIntent.AddFriend,
            ConsoleIntent.AdvanceToNextTier,
            is ConsoleIntent.SetPlan,
            is ConsoleIntent.SetLatency,
            ConsoleIntent.ToggleFailNextLoad,
            ConsoleIntent.ToggleFailNextClaim -> save(intent)
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            controls.state
                .catch { if (it is IOException) onIntent(ConsoleIntent.LoadFailed) else throw it }
                .collect { onIntent(ConsoleIntent.Loaded(it)) }
        }
    }

    private fun save(intent: ConsoleIntent) {
        viewModelScope.launch {
            try {
                mutex.withLock { handle(intent) }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to save demo state for $intent", e)
            }
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

            ConsoleIntent.ToggleFailNextClaim -> controls.setFailNextClaim(!debug.failNextClaim)

            is ConsoleIntent.Reset -> controls.reset()

            ConsoleIntent.CancelReset,
            ConsoleIntent.OpenReferrals,
            ConsoleIntent.Retry,
            is ConsoleIntent.Loaded,
            ConsoleIntent.LoadFailed -> Unit
        }
    }

    private companion object {
        const val TAG = "ConsoleViewModel"
    }
}

internal fun reduce(state: ConsoleState, intent: ConsoleIntent): ConsoleState {
    val load = state.load
    return when (intent) {
        ConsoleIntent.Retry -> if (load is ConsoleLoad.Failed) ConsoleState(ConsoleLoad.Loading) else state

        is ConsoleIntent.Loaded -> ConsoleState(
            ConsoleLoad.Ready(intent.debug, isConfirmingReset = (load as? ConsoleLoad.Ready)?.isConfirmingReset == true)
        )

        ConsoleIntent.LoadFailed -> ConsoleState(ConsoleLoad.Failed)

        is ConsoleIntent.Reset -> if (load is ConsoleLoad.Ready) {
            ConsoleState(load.copy(isConfirmingReset = !load.isConfirmingReset))
        } else {
            state
        }

        ConsoleIntent.CancelReset -> if (load is ConsoleLoad.Ready) {
            ConsoleState(load.copy(isConfirmingReset = false))
        } else {
            state
        }

        else -> state
    }
}

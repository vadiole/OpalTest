package vadiole.opaltest.feature.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vadiole.opaltest.data.referral.ReferralRepository
import vadiole.opaltest.domain.Outcome
import vadiole.opaltest.domain.referral.ReferralError
import vadiole.opaltest.domain.referral.ReferralProgram

class ReferralViewModel(private val repository: ReferralRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<ReferralState>(ReferralState.Loading)
    val state: StateFlow<ReferralState> = mutableState.asStateFlow()

    private val effectChannel = Channel<ReferralEffect>(Channel.BUFFERED)
    val effects: Flow<ReferralEffect> = effectChannel.receiveAsFlow()

    private var loadJob: Job? = null
    private var shareCooldown: Job? = null

    init {
        load()
    }

    fun onIntent(intent: ReferralIntent) {
        when (intent) {
            ReferralIntent.Retry -> load()
            ReferralIntent.Share -> share()
            ReferralIntent.CopyInvite -> invite()?.let { effectChannel.trySend(ReferralEffect.CopyInvite(it)) }
            is ReferralIntent.Claim -> claim(intent.tierId)
        }
    }

    private fun invite(): Invite? =
        (state.value as? ReferralState.Ready)?.program?.let { Invite(it.code, it.shareUrl, it.inviteeTrialDays) }

    private fun share() {
        if (shareCooldown?.isActive == true) return
        val invite = invite() ?: return
        effectChannel.trySend(ReferralEffect.Share(invite))
        shareCooldown = viewModelScope.launch { delay(SHARE_COOLDOWN) }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        onResult(ReferralResult.LoadStarted)
        loadJob = viewModelScope.launch {
            when (val outcome = repository.getProgram()) {
                is Outcome.Success -> onResult(ReferralResult.Loaded(outcome.value))
                is Outcome.Failure -> onResult(ReferralResult.LoadFailed(outcome.error))
            }
        }
    }

    private fun claim(tierId: String) {
        val ready = state.value as? ReferralState.Ready ?: return
        if (ready.claimingTierId != null) return
        val reward = ready.program.tiers.firstOrNull { it.id == tierId }?.reward ?: return
        onResult(ReferralResult.ClaimStarted(tierId))
        viewModelScope.launch {
            when (val outcome = repository.claimReward(tierId)) {
                is Outcome.Success -> {
                    loadJob?.cancel()
                    onResult(ReferralResult.Claimed(outcome.value))
                    effectChannel.send(ReferralEffect.ClaimSucceeded(reward))
                }

                is Outcome.Failure -> {
                    onResult(ReferralResult.ClaimFailed)
                    effectChannel.send(ReferralEffect.ClaimFailed(outcome.error))
                    if (outcome.error == ReferralError.AlreadyClaimed ||
                        outcome.error == ReferralError.NotEligible
                    ) {
                        load()
                    }
                }
            }
        }
    }

    private fun onResult(result: ReferralResult) = mutableState.update { reduce(it, result) }

    companion object {
        val SHARE_COOLDOWN = 1.seconds
    }
}

internal sealed interface ReferralResult {
    data object LoadStarted : ReferralResult

    data class Loaded(val program: ReferralProgram) : ReferralResult

    data class LoadFailed(val error: ReferralError) : ReferralResult

    data class ClaimStarted(val tierId: String) : ReferralResult

    data class Claimed(val program: ReferralProgram) : ReferralResult

    data object ClaimFailed : ReferralResult
}

internal fun reduce(state: ReferralState, result: ReferralResult): ReferralState = when (result) {
    ReferralResult.LoadStarted -> if (state is ReferralState.Ready) state else ReferralState.Loading
    is ReferralResult.Loaded -> ReferralState.Ready(result.program, (state as? ReferralState.Ready)?.claimingTierId)
    is ReferralResult.LoadFailed -> if (state is ReferralState.Ready) state else ReferralState.Failed(result.error)
    is ReferralResult.ClaimStarted -> (state as? ReferralState.Ready)?.copy(claimingTierId = result.tierId) ?: state
    is ReferralResult.Claimed -> ReferralState.Ready(result.program)
    ReferralResult.ClaimFailed -> (state as? ReferralState.Ready)?.copy(claimingTierId = null) ?: state
}

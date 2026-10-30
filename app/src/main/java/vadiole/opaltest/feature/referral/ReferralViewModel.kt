package vadiole.opaltest.feature.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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

class ReferralViewModel(private val repository: ReferralRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ReferralState())
    val state: StateFlow<ReferralState> = mutableState.asStateFlow()

    private val effectChannel = Channel<ReferralEffect>(Channel.BUFFERED)
    val effects: Flow<ReferralEffect> = effectChannel.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onIntent(intent: ReferralIntent) {
        when (intent) {
            ReferralIntent.Retry -> load()

            ReferralIntent.Share -> state.value.program?.let {
                effectChannel.trySend(ReferralEffect.Share(it.code, it.shareUrl, it.inviteeTrialDays))
            }

            is ReferralIntent.Claim -> claim(intent.tierId)
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        mutableState.update { it.copy(loadError = null) }
        loadJob = viewModelScope.launch {
            when (val outcome = repository.getProgram()) {
                is Outcome.Success -> mutableState.update { it.copy(program = outcome.value) }
                is Outcome.Failure -> mutableState.update { it.copy(loadError = outcome.error) }
            }
        }
    }

    private fun claim(tierId: String) {
        if (state.value.claimingTierId != null) return
        mutableState.update { it.copy(claimingTierId = tierId) }
        viewModelScope.launch {
            when (val outcome = repository.claimReward(tierId)) {
                is Outcome.Success -> {
                    mutableState.update { it.copy(program = outcome.value, claimingTierId = null) }
                    effectChannel.send(ReferralEffect.ClaimSucceeded)
                }

                is Outcome.Failure -> {
                    mutableState.update { it.copy(claimingTierId = null) }
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
}

package vadiole.opaltest.feature.console

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import vadiole.opaltest.data.debug.ReferralDebugState
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

data class ConsoleState(val load: ConsoleLoad = ConsoleLoad.Loading)

sealed interface ConsoleLoad {
    data object Loading : ConsoleLoad

    data object Failed : ConsoleLoad

    data class Ready(val debug: ReferralDebugState, val isConfirmingReset: Boolean = false) : ConsoleLoad
}

sealed interface ConsoleIntent {
    data object AddFriend : ConsoleIntent
    data object AdvanceToNextTier : ConsoleIntent
    data class SetPlan(val plan: Plan) : ConsoleIntent
    data class SetLatency(val latency: SimulatedLatency) : ConsoleIntent
    data object ToggleFailNextLoad : ConsoleIntent
    data object ToggleFailNextClaim : ConsoleIntent
    data class Reset(val confirmTimeout: Duration = RESET_CONFIRM_TIMEOUT) : ConsoleIntent
    data object CancelReset : ConsoleIntent
    data object OpenReferrals : ConsoleIntent
    data object Retry : ConsoleIntent
    data class Loaded(val debug: ReferralDebugState) : ConsoleIntent
    data object LoadFailed : ConsoleIntent
}

sealed interface ConsoleEffect {
    data object FriendAdded : ConsoleEffect
    data object TierReached : ConsoleEffect
    data object OpenReferrals : ConsoleEffect
}

val RESET_CONFIRM_TIMEOUT = 4.seconds

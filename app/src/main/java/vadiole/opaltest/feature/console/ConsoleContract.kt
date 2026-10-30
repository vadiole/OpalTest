package vadiole.opaltest.feature.console

import vadiole.opaltest.data.fake.ReferralDebugState
import vadiole.opaltest.data.fake.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

data class ConsoleState(
    val debug: ReferralDebugState? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isConfirmingReset: Boolean = false
)

sealed interface ConsoleIntent {
    data object AddFriend : ConsoleIntent
    data object AdvanceToNextTier : ConsoleIntent
    data class SetPlan(val plan: Plan) : ConsoleIntent
    data class SetLatency(val latency: SimulatedLatency) : ConsoleIntent
    data object ToggleFailNextLoad : ConsoleIntent
    data object Reset : ConsoleIntent
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

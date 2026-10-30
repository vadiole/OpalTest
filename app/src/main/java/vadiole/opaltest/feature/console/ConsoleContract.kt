package vadiole.opaltest.feature.console

import vadiole.opaltest.data.fake.ReferralDebugState
import vadiole.opaltest.data.fake.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

data class ConsoleState(val debug: ReferralDebugState? = null)

sealed interface ConsoleIntent {
    data object AddFriend : ConsoleIntent
    data object AdvanceToNextTier : ConsoleIntent
    data class SetPlan(val plan: Plan) : ConsoleIntent
    data class SetLatency(val latency: SimulatedLatency) : ConsoleIntent
    data object ToggleFailNextLoad : ConsoleIntent
    data object Reset : ConsoleIntent
    data object OpenReferrals : ConsoleIntent
}

sealed interface ConsoleEffect {
    data object FriendAdded : ConsoleEffect
    data object TierReached : ConsoleEffect
    data object OpenReferrals : ConsoleEffect
}

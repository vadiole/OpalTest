package vadiole.opaltest.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import vadiole.opaltest.feature.console.ConsoleScreen
import vadiole.opaltest.feature.referral.ReferralScreen
import vadiole.opaltest.ui.common.rememberStackTransitions

@Serializable
data object ConsoleRoute : NavKey

@Serializable
data object ReferralRoute : NavKey

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ConsoleRoute)
    val transitions = rememberStackTransitions(backStack)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            transitions.onBackCompleted()
            backStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            transitions.decorator
        ),
        transitionSpec = { transitions.push() },
        popTransitionSpec = { transitions.pop() },
        predictivePopTransitionSpec = { transitions.predictivePop() },
        entryProvider = entryProvider {
            entry<ConsoleRoute> {
                ConsoleScreen(onOpenReferrals = { backStack.add(ReferralRoute) })
            }
            entry<ReferralRoute> {
                ReferralScreen()
            }
        }
    )
}

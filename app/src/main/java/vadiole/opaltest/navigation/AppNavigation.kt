package vadiole.opaltest.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.serialization.Serializable
import vadiole.opaltest.feature.console.ConsoleScreen
import vadiole.opaltest.feature.referral.ReferralScreen
import vadiole.opaltest.ui.theme.OpalTheme

@Serializable
data object ConsoleRoute : NavKey

@Serializable
data object ReferralRoute : NavKey

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ConsoleRoute)
    val transitions = rememberStackTransitions(backStack)
    val navigate: (NavKey) -> Unit = { route ->
        if (!transitions.isTransitioning && backStack.lastOrNull() != route) backStack.add(route)
    }
    val navigateBack: () -> Unit = {
        if (!transitions.isTransitioning && backStack.size > 1) {
            transitions.onBackCompleted()
            backStack.removeAt(backStack.lastIndex)
        }
    }
    StackNavDisplay(
        backStack = backStack,
        transitions = transitions,
        onBack = navigateBack,
        modifier = modifier.background(OpalTheme.colors.background),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ConsoleRoute> {
                ConsoleScreen(onOpenReferrals = { navigate(ReferralRoute) })
            }
            entry<ReferralRoute> {
                ReferralScreen(onBack = navigateBack)
            }
        }
    )
}

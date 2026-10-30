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

@Serializable
data object ConsoleRoute : NavKey

@Serializable
data object ReferralRoute : NavKey

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ConsoleRoute)
    val navigateBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = navigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ConsoleRoute> {
                ConsoleScreen(onOpenReferrals = { if (backStack.last() != ReferralRoute) backStack.add(ReferralRoute) })
            }
            entry<ReferralRoute> {
                ReferralScreen(onBack = navigateBack)
            }
        }
    )
}

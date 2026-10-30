package vadiole.opaltest.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vadiole.opaltest.ui.theme.OpalTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpalComponentSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun `single-select chip is a radio button and toggle chip is a checkbox`() {
        compose.setContent {
            OpalTheme {
                Column {
                    OpalChip(text = "Free", selected = true, onClick = {})
                    OpalToggleChip(text = "Fail", checked = true, onCheckedChange = {})
                }
            }
        }
        compose.onNodeWithText("Free").assert(hasRole(Role.RadioButton)).assertIsSelected()
        compose.onNodeWithText("Fail").assert(hasRole(Role.Checkbox)).assertIsOn()
    }

    @Test
    fun `loading button is one busy node without a separate label`() {
        compose.setContent {
            OpalTheme { OpalButton(text = "Claim", onClick = {}, loading = true) }
        }
        compose.onNodeWithContentDescription("Claim")
            .assert(hasRole(Role.Button))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
            .assertIsNotEnabled()
        compose.onNodeWithText("Claim").assertDoesNotExist()
    }
}

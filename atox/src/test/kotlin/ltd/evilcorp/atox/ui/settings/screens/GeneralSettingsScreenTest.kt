package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = ["androidx.loader.content"], sdk = [34])
class GeneralSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGeneralSettingsScreenDisplaysElements() {
        composeTestRule.setContent {
            GeneralSettingsScreen(
                paddingValues = PaddingValues(0.dp),
                showProfilePicker = true,
                runAtStartup = true,
                confirmQuitting = true,
                performHaptic = {},
                onShowProfilePickerChanged = {},
                onRunAtStartupChanged = {},
                onConfirmQuittingChanged = {}
            )
        }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_heading_general)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_show_profile_picker)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_run_at_startup)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_confirm_quitting)).performScrollTo().assertIsDisplayed()
    }
}

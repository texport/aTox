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
class PrivacySettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPrivacySettingsScreenDisplaysElements() {
        composeTestRule.setContent {
            PrivacySettingsScreen(
                paddingValues = PaddingValues(0.dp),
                disableScreenshots = true,
                confirmCalling = true,
                isBiometricHardwareAvailable = true,
                biometricEnabled = true,
                hasPassword = true,
                performHaptic = {},
                onDisableScreenshotsChanged = {},
                onConfirmCallingChanged = {},
                onBiometricEnabledChanged = {},
                onChangePasswordClick = {}
            )
        }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.settings_privacy_group)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_block_screenshots)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_confirm_calling)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_biometric_login)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(ltd.evilcorp.atox.R.string.pref_heading_change_password)).performScrollTo().assertIsDisplayed()
    }
}

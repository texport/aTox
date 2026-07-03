package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private const val DIVIDER_ALPHA = 0.08f

@Composable
fun GeneralSettingsScreen(
    paddingValues: PaddingValues,
    showProfilePicker: Boolean,
    runAtStartup: Boolean,
    confirmQuitting: Boolean,
    performHaptic: () -> Unit,
    onShowProfilePickerChanged: (Boolean) -> Unit,
    onRunAtStartupChanged: (Boolean) -> Unit,
    onConfirmQuittingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = ltd.evilcorp.atox.ui.navigation.LocalTabPadding.current.calculateBottomPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = HorizontalMargin),
        verticalArrangement = Arrangement.spacedBy(SpacingSpacedBy),
        contentPadding = PaddingValues(
            top = ContentPaddingTop,
            bottom = ContentPaddingBottomDefault + bottomPadding
        )
    ) {
        item {
            SettingsGroup(title = stringResource(R.string.pref_heading_general)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_show_profile_picker),
                    subtitle = stringResource(R.string.pref_show_profile_picker_description),
                    checked = showProfilePicker
                ) { checked ->
                    performHaptic()
                    onShowProfilePickerChanged(checked)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_run_at_startup),
                    subtitle = stringResource(R.string.settings_start_on_boot_sub),
                    checked = runAtStartup
                ) { checked ->
                    performHaptic()
                    onRunAtStartupChanged(checked)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_confirm_quitting),
                    subtitle = stringResource(R.string.quit_confirm),
                    checked = confirmQuitting
                ) { checked ->
                    performHaptic()
                    onConfirmQuittingChanged(checked)
                }
            }
        }
    }
}

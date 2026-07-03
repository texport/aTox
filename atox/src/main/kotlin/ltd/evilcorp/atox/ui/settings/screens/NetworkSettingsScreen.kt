// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.settings.common.SettingsClickableRow
import ltd.evilcorp.atox.ui.settings.common.SettingsGroup
import ltd.evilcorp.atox.ui.settings.common.SettingsSwitchRow
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyType

private val ContentPaddingTop = 16.dp
private val ContentPaddingBottomDefault = 32.dp
private val HorizontalMargin = 16.dp
private val SpacingSpacedBy = 16.dp
private const val DIVIDER_ALPHA = 0.08f
private const val DISABLED_ALPHA = 0.38f

@Suppress("FunctionNaming")
@Composable
fun NetworkSettingsScreen(
    paddingValues: PaddingValues,
    udpEnabled: Boolean,
    bootstrapNodeSource: BootstrapNodeSource,
    proxyType: ProxyType,
    proxyAddress: String,
    proxyPortInput: String,
    focusManager: FocusManager,
    performHaptic: () -> Unit,
    onUdpEnabledChanged: (Boolean) -> Unit,
    onBootstrapNodesClick: () -> Unit,
    onProxyTypeClick: () -> Unit,
    onProxyAddressChanged: (String) -> Unit,
    onProxyPortInputChanged: (String) -> Unit,
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
            SettingsGroup(title = stringResource(R.string.settings_network_group)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.pref_udp_enabled),
                    subtitle = stringResource(R.string.pref_udp_enabled),
                    checked = udpEnabled
                ) { checked ->
                    performHaptic()
                    onUdpEnabledChanged(checked)
                }
                val bootstrapLabel = when (bootstrapNodeSource) {
                    BootstrapNodeSource.BuiltIn -> stringResource(R.string.settings_nodes_builtin)
                    BootstrapNodeSource.UserProvided -> stringResource(R.string.settings_nodes_user)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.settings_nodes_list),
                    subtitle = bootstrapLabel
                ) {
                    performHaptic()
                    onBootstrapNodesClick()
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.settings_proxy_group)) {
                val proxyLabel = when (proxyType) {
                    ProxyType.None -> stringResource(R.string.pref_proxy_type_none)
                    ProxyType.HTTP -> stringResource(R.string.pref_proxy_type_http)
                    ProxyType.SOCKS5 -> stringResource(R.string.pref_proxy_type_socks5)
                }
                SettingsClickableRow(
                    title = stringResource(R.string.settings_proxy_type),
                    subtitle = proxyLabel
                ) {
                    performHaptic()
                    onProxyTypeClick()
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
                val isProxyEnabled = proxyType != ProxyType.None
                val fieldsAlpha = if (isProxyEnabled) 1.0f else DISABLED_ALPHA
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .alpha(fieldsAlpha)
                ) {
                    OutlinedTextField(
                        value = proxyAddress,
                        onValueChange = onProxyAddressChanged,
                        enabled = isProxyEnabled,
                        label = { Text(stringResource(R.string.settings_proxy_address)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = proxyPortInput,
                        onValueChange = onProxyPortInputChanged,
                        enabled = isProxyEnabled,
                        label = { Text(stringResource(R.string.settings_proxy_port)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

package ltd.evilcorp.atox.ui.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.common.AtoxPasswordField

@Composable
fun ChangePasswordDialog(
    hasPassword: Boolean,
    onConfirm: (current: String, new: String) -> Boolean,
    onDismiss: () -> Unit,
    focusManager: FocusManager
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    val passwordsMustMatch = stringResource(R.string.passwords_must_match)
    val passwordCannotBeEmpty = stringResource(R.string.password_cannot_be_empty)
    val incorrectPassword = stringResource(R.string.incorrect_password)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (hasPassword) R.string.pref_heading_change_password else R.string.pref_heading_set_password
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasPassword) {
                    AtoxPasswordField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorText = ""
                        },
                        label = stringResource(R.string.pref_current_password),
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AtoxPasswordField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorText = ""
                    },
                    label = stringResource(R.string.pref_new_password),
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AtoxPasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = ""
                    },
                    label = stringResource(R.string.pref_confirm_password),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword.isEmpty()) {
                        errorText = passwordCannotBeEmpty
                        return@TextButton
                    }
                    if (newPassword != confirmPassword) {
                        errorText = passwordsMustMatch
                        return@TextButton
                    }
                    val success = onConfirm(currentPassword, newPassword)
                    if (success) {
                        onDismiss()
                    } else {
                        errorText = incorrectPassword
                    }
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reject))
            }
        }
    )
}

// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.ReactionParser

@Composable
fun MessageContextDropdown(
    msg: Message,
    uiConfig: ChatUiConfig,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onCopyMessage: ((Message) -> Unit)?,
    onReplyMessage: ((Message) -> Unit)?,
    onForwardMessage: ((Message) -> Unit)?,
    onReact: ((Message, String) -> Unit)? = null,
    isAction: Boolean
) {
    val hasMenuItems = onCopyMessage != null || onReplyMessage != null || onForwardMessage != null
    val hasReactions = onReact != null

    if (!hasMenuItems && !hasReactions) return

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest
    ) {
        if (hasReactions) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                for (emoji in ReactionParser.VALID_EMOJIS) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDismissRequest()
                                onReact(msg, emoji)
                            }
                            .padding(4.dp),
                        maxLines = 1
                    )
                }
            }
            if (hasMenuItems) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        if (msg.type != MessageType.FileTransfer && !isAction && onCopyMessage != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy)) },
                onClick = {
                    onDismissRequest()
                    onCopyMessage(msg)
                }
            )
        }
        if (!isAction) {
            if (uiConfig.enableReplies && onReplyMessage != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reply)) },
                    onClick = {
                        onDismissRequest()
                        onReplyMessage(msg)
                    }
                )
            }
            if (onForwardMessage != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.forward)) },
                    onClick = {
                        onDismissRequest()
                        onForwardMessage(msg)
                    }
                )
            }
        }
    }
}

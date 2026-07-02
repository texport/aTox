package ltd.evilcorp.atox.ui.common.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ltd.evilcorp.atox.ui.chat.ChatUiConfig
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message

@Composable
fun AudioMessageBubble(
    ft: FileTransfer,
    contentColor: Color,
    onAcceptFt: (Int) -> Unit,
    onRejectFt: (Int) -> Unit,
    msg: Message,
    isOutgoing: Boolean,
    uiConfig: ChatUiConfig
) {
    AudioMessageCard(
        ft = ft,
        contentColor = contentColor,
        onAcceptFt = onAcceptFt,
        onRejectFt = onRejectFt,
        msg = msg,
        isOutgoing = isOutgoing,
        uiConfig = uiConfig
    )
}

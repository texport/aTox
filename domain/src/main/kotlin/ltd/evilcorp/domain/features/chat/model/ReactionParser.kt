// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat.model

data class ReactionInfo(
    val isReaction: Boolean,
    val parentIdentifier: String,
    val emoji: String
)

object ReactionParser {
    val VALID_EMOJIS = listOf("\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDE2D", "\uD83D\uDE31", "\uD83D\uDD25", "\uD83D\uDCA9")

    fun parse(messageText: String): ReactionInfo {
        if (messageText.startsWith("[reaction:")) {
            val bracketEnd = messageText.indexOf(']')
            if (bracketEnd != -1) {
                val parentIdentifier = messageText.substring(10, bracketEnd)
                val emoji = messageText.substring(bracketEnd + 1)
                if (emoji in VALID_EMOJIS) {
                    return ReactionInfo(
                        isReaction = true,
                        parentIdentifier = parentIdentifier,
                        emoji = emoji
                    )
                }
            }
        }
        return ReactionInfo(isReaction = false, parentIdentifier = "", emoji = "")
    }

    fun isReaction(messageText: String): Boolean {
        return parse(messageText).isReaction
    }

    fun buildMessage(parentHashCode: Int, emoji: String): String {
        return "[reaction:$parentHashCode]$emoji"
    }
}

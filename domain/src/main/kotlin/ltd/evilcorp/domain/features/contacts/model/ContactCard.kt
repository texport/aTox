// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.model

/**
 * Represents a contact card that can be shared and imported.
 * Contains only the Tox ID needed to add the contact.
 */
data class ContactCard(
    val toxId: String,
    val displayName: String = ""
)

object ContactCardParser {
    private const val PREFIX = "[ATOX_CONTACT:"
    private const val SUFFIX = "]"

    fun encode(toxId: String, displayName: String = ""): String {
        return if (displayName.isNotEmpty()) {
            "$PREFIX$toxId|$displayName$SUFFIX"
        } else {
            "$PREFIX$toxId$SUFFIX"
        }
    }

    fun decode(message: String): ContactCard? {
        if (!message.startsWith(PREFIX) || !message.endsWith(SUFFIX)) {
            return null
        }

        val content = message.substring(PREFIX.length, message.length - SUFFIX.length)
        val parts = content.split("|")

        return when (parts.size) {
            1 -> ContactCard(toxId = parts[0].trim())
            2 -> ContactCard(toxId = parts[0].trim(), displayName = parts[1].trim())
            else -> null
        }
    }

    fun isContactCard(message: String): Boolean {
        return message.startsWith(PREFIX) && message.endsWith(SUFFIX)
    }
}

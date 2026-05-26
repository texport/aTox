package ltd.evilcorp.domain.feature

import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.PublicKey

interface NotificationManager {
    fun showOngoingCallNotification(contact: Contact)
    fun dismissCallNotification(publicKey: PublicKey)
}

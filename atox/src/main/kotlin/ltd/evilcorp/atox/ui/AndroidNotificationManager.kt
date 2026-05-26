package ltd.evilcorp.atox.ui

import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.feature.NotificationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNotificationManager @Inject constructor(
    private val notificationHelper: NotificationHelper
) : NotificationManager {
    override fun showOngoingCallNotification(contact: Contact) = notificationHelper.showOngoingCallNotification(contact)
    override fun dismissCallNotification(publicKey: PublicKey) = notificationHelper.dismissCallNotification(publicKey)
}

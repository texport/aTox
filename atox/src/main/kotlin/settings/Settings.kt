package ltd.evilcorp.atox.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.AppSound
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.atox.receiver.BootReceiver
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.model.UserSettings
import ltd.evilcorp.core.repository.UserSettingsRepository

class Settings @Inject constructor(
    private val ctx: Context,
    private val repository: UserSettingsRepository,
) {
    val state: StateFlow<UserSettings> = repository.settings

    init {
        val persisted = repository.settings.value.runAtStartup
        val actual = isBootReceiverEnabled()
        if (persisted != actual) {
            repository.updateRunAtStartup(actual)
        }
    }

    var udpEnabled: Boolean
        get() = repository.settings.value.udpEnabled
        set(enabled) = repository.updateUdpEnabled(enabled)

    var runAtStartup: Boolean
        get() = repository.settings.value.runAtStartup
        set(runAtStartup) {
            repository.updateRunAtStartup(runAtStartup)
            val state = if (runAtStartup) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            ctx.packageManager.setComponentEnabledSetting(
                ComponentName(ctx, BootReceiver::class.java),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }

    var autoAwayEnabled: Boolean
        get() = repository.settings.value.autoAwayEnabled
        set(enabled) = repository.updateAutoAwayEnabled(enabled)

    var autoAwaySeconds: Long
        get() = repository.settings.value.autoAwaySeconds
        set(seconds) = repository.updateAutoAwaySeconds(seconds)

    var proxyType: ProxyType
        get() = repository.settings.value.proxyType
        set(type) = repository.updateProxyType(type)

    var proxyAddress: String
        get() = repository.settings.value.proxyAddress
        set(address) = repository.updateProxyAddress(address)

    var dateFormatPreference: DateFormatPreference
        get() = repository.settings.value.dateFormatPreference
        set(preference) = repository.updateDateFormatPreference(preference)

    var timeFormatPreference: TimeFormatPreference
        get() = repository.settings.value.timeFormatPreference
        set(preference) = repository.updateTimeFormatPreference(preference)

    var proxyPort: Int
        get() = repository.settings.value.proxyPort
        set(port) = repository.updateProxyPort(port)

    var ftAutoAccept: FtAutoAccept
        get() = repository.settings.value.ftAutoAccept
        set(autoAccept) = repository.updateFtAutoAccept(autoAccept)

    var bootstrapNodeSource: BootstrapNodeSource
        get() = repository.settings.value.bootstrapNodeSource
        set(source) = repository.updateBootstrapNodeSource(source)

    var disableScreenshots: Boolean
        get() = repository.settings.value.disableScreenshots
        set(disable) = repository.updateDisableScreenshots(disable)

    var confirmQuitting: Boolean
        get() = repository.settings.value.confirmQuitting
        set(confirm) = repository.updateConfirmQuitting(confirm)

    var confirmCalling: Boolean
        get() = repository.settings.value.confirmCalling
        set(confirm) = repository.updateConfirmCalling(confirm)

    var sentMessageSoundVolume: Int
        get() = repository.settings.value.sentMessageSoundVolume
        set(volume) = repository.updateSentMessageSoundVolume(volume)

    var sentMessageSoundUri: String
        get() = repository.settings.value.sentMessageSoundUri
        set(uri) = repository.updateSentMessageSoundUri(uri)

    var callSound: AppSound
        get() = repository.settings.value.callSound
        set(sound) = repository.updateCallSound(sound)

    var callSoundVolume: Int
        get() = repository.settings.value.callSoundVolume
        set(volume) = repository.updateCallSoundVolume(volume)

    var callRingtoneUri: String
        get() = repository.settings.value.callRingtoneUri
        set(uri) = repository.updateCallRingtoneUri(uri)

    var notificationSoundVolume: Int
        get() = repository.settings.value.notificationSoundVolume
        set(volume) = repository.updateNotificationSoundVolume(volume)

    var notificationSoundUri: String
        get() = repository.settings.value.notificationSoundUri
        set(uri) = repository.updateNotificationSoundUri(uri)

    var activeChatSoundVolume: Int
        get() = repository.settings.value.activeChatSoundVolume
        set(volume) = repository.updateActiveChatSoundVolume(volume)

    var activeChatSoundUri: String
        get() = repository.settings.value.activeChatSoundUri
        set(uri) = repository.updateActiveChatSoundUri(uri)

    var hapticEnabled: Boolean
        get() = repository.settings.value.hapticEnabled
        set(enabled) = repository.updateHapticEnabled(enabled)

    var autoSaveToDownloads: Boolean
        get() = repository.settings.value.autoSaveToDownloads
        set(enabled) = repository.updateAutoSaveToDownloads(enabled)

    private fun isBootReceiverEnabled(): Boolean =
        ctx.packageManager.getComponentEnabledSetting(
            ComponentName(ctx, BootReceiver::class.java),
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
}

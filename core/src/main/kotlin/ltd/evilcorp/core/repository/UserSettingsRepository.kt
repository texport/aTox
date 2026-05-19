package ltd.evilcorp.core.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.DEFAULT_ACCENT_COLOR_SEED
import ltd.evilcorp.core.model.DEFAULT_THEME_MODE
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.AppSound
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.core.model.UserSettings
import ltd.evilcorp.core.tox.save.ProxyType

private val Context.userSettingsDataStore by preferencesDataStore(
    name = "user_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
            ),
        )
    },
)

@Singleton
class UserSettingsRepository @Inject constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: StateFlow<UserSettings> = context.userSettingsDataStore.data
        .map(::toUserSettings)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = readBlocking(context),
        )

    fun updateThemeMode(themeMode: Int) = update(Keys.themeMode, themeMode)

    fun updateDynamicColorEnabled(enabled: Boolean) = update(Keys.dynamicColorEnabled, enabled)

    fun updateAccentColorSeed(accentColorSeed: Int) = update(Keys.accentColorSeed, accentColorSeed)

    fun updateLocaleTag(localeTag: String) = update(Keys.localeTag, localeTag)

    fun updateDateFormatPreference(preference: DateFormatPreference) =
        update(Keys.dateFormatPreferenceOrdinal, preference.ordinal)

    fun updateTimeFormatPreference(preference: TimeFormatPreference) =
        update(Keys.timeFormatPreferenceOrdinal, preference.ordinal)

    fun updateUdpEnabled(enabled: Boolean) = update(Keys.udpEnabled, enabled)

    fun updateRunAtStartup(enabled: Boolean) = update(Keys.runAtStartup, enabled)

    fun updateAutoAwayEnabled(enabled: Boolean) = update(Keys.autoAwayEnabled, enabled)

    fun updateAutoAwaySeconds(seconds: Long) = update(Keys.autoAwaySeconds, seconds)

    fun updateProxyType(type: ProxyType) = update(Keys.proxyTypeOrdinal, type.ordinal)

    fun updateProxyAddress(address: String) = update(Keys.proxyAddress, address)

    fun updateProxyPort(port: Int) = update(Keys.proxyPort, port)

    fun updateFtAutoAccept(value: FtAutoAccept) = update(Keys.ftAutoAcceptOrdinal, value.ordinal)

    fun updateBootstrapNodeSource(value: BootstrapNodeSource) = update(Keys.bootstrapNodeSourceOrdinal, value.ordinal)

    fun updateDisableScreenshots(disable: Boolean) = update(Keys.disableScreenshots, disable)

    fun updateConfirmQuitting(confirm: Boolean) = update(Keys.confirmQuitting, confirm)

    fun updateConfirmCalling(confirm: Boolean) = update(Keys.confirmCalling, confirm)

    fun updateSentMessageSoundVolume(volume: Int) = update(Keys.sentMessageSoundVolume, volume.coerceIn(0, 100))
    fun updateSentMessageSoundUri(uri: String) = update(Keys.sentMessageSoundUri, uri)

    fun updateCallSound(sound: AppSound) = update(Keys.callSoundOrdinal, sound.ordinal)

    fun updateCallSoundVolume(volume: Int) = update(Keys.callSoundVolume, volume.coerceIn(0, 100))

    fun updateCallRingtoneUri(uri: String) = update(Keys.callRingtoneUri, uri)

    fun updateNotificationSoundVolume(volume: Int) = update(Keys.notificationSoundVolume, volume.coerceIn(0, 100))
    fun updateNotificationSoundUri(uri: String) = update(Keys.notificationSoundUri, uri)

    fun updateActiveChatSoundVolume(volume: Int) = update(Keys.activeChatSoundVolume, volume.coerceIn(0, 100))
    fun updateActiveChatSoundUri(uri: String) = update(Keys.activeChatSoundUri, uri)

    fun updateHapticEnabled(enabled: Boolean) = update(Keys.hapticEnabled, enabled)

    fun updateAutoSaveToDownloads(enabled: Boolean) = update(Keys.autoSaveToDownloads, enabled)

    private fun <T> update(key: Preferences.Key<T>, value: T) {
        scope.launch {
            context.userSettingsDataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    companion object {
        fun readBlocking(context: Context): UserSettings = runBlocking {
            toUserSettings(context.userSettingsDataStore.data.first())
        }

        private fun toUserSettings(preferences: Preferences): UserSettings =
            UserSettings(
                themeMode = preferences[Keys.themeMode] ?: DEFAULT_THEME_MODE,
                dynamicColorEnabled = preferences[Keys.dynamicColorEnabled] ?: true,
                accentColorSeed = preferences[Keys.accentColorSeed] ?: DEFAULT_ACCENT_COLOR_SEED,
                localeTag = preferences[Keys.localeTag] ?: "",
                dateFormatPreference = DateFormatPreference.entries[
                    preferences[Keys.dateFormatPreferenceOrdinal] ?: DateFormatPreference.System.ordinal
                ],
                timeFormatPreference = TimeFormatPreference.entries[
                    preferences[Keys.timeFormatPreferenceOrdinal] ?: TimeFormatPreference.System.ordinal
                ],
                udpEnabled = preferences[Keys.udpEnabled] ?: false,
                runAtStartup = preferences[Keys.runAtStartup] ?: false,
                autoAwayEnabled = preferences[Keys.autoAwayEnabled] ?: false,
                autoAwaySeconds = preferences[Keys.autoAwaySeconds] ?: 180L,
                proxyType = ProxyType.entries[preferences[Keys.proxyTypeOrdinal] ?: ProxyType.None.ordinal],
                proxyAddress = preferences[Keys.proxyAddress] ?: "",
                proxyPort = preferences[Keys.proxyPort] ?: 0,
                ftAutoAccept = FtAutoAccept.entries[preferences[Keys.ftAutoAcceptOrdinal] ?: FtAutoAccept.None.ordinal],
                bootstrapNodeSource = BootstrapNodeSource.entries[
                    preferences[Keys.bootstrapNodeSourceOrdinal] ?: BootstrapNodeSource.BuiltIn.ordinal
                ],
                disableScreenshots = preferences[Keys.disableScreenshots] ?: false,
                confirmQuitting = preferences[Keys.confirmQuitting] ?: true,
                confirmCalling = preferences[Keys.confirmCalling] ?: true,
                sentMessageSoundVolume = preferences[Keys.sentMessageSoundVolume] ?: 24,
                sentMessageSoundUri = preferences[Keys.sentMessageSoundUri] ?: "",
                callSound = AppSound.entries[
                    preferences[Keys.callSoundOrdinal] ?: AppSound.Pulse.ordinal
                ],
                callSoundVolume = preferences[Keys.callSoundVolume] ?: 72,
                callRingtoneUri = preferences[Keys.callRingtoneUri] ?: "",
                notificationSoundVolume = preferences[Keys.notificationSoundVolume] ?: 52,
                notificationSoundUri = preferences[Keys.notificationSoundUri] ?: "",
                activeChatSoundVolume = preferences[Keys.activeChatSoundVolume] ?: 28,
                activeChatSoundUri = preferences[Keys.activeChatSoundUri] ?: "",
                hapticEnabled = preferences[Keys.hapticEnabled] ?: true,
                autoSaveToDownloads = preferences[Keys.autoSaveToDownloads] ?: true,
            )
    }

    private object Keys {
        val themeMode = intPreferencesKey("theme")
        val dynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val accentColorSeed = intPreferencesKey("accent_color_seed")
        val localeTag = stringPreferencesKey("locale_tag")
        val dateFormatPreferenceOrdinal = intPreferencesKey("date_format_preference")
        val timeFormatPreferenceOrdinal = intPreferencesKey("time_format_preference")
        val udpEnabled = booleanPreferencesKey("udp_enabled")
        val runAtStartup = booleanPreferencesKey("run_at_startup")
        val autoAwayEnabled = booleanPreferencesKey("auto_away_enabled")
        val autoAwaySeconds = longPreferencesKey("auto_away_seconds")
        val proxyTypeOrdinal = intPreferencesKey("proxy_type")
        val proxyAddress = stringPreferencesKey("proxy_address")
        val proxyPort = intPreferencesKey("proxy_port")
        val ftAutoAcceptOrdinal = intPreferencesKey("ft_auto_accept")
        val bootstrapNodeSourceOrdinal = intPreferencesKey("bootstrap_node_source")
        val disableScreenshots = booleanPreferencesKey("disable_screenshots")
        val confirmQuitting = booleanPreferencesKey("confirm_quitting")
        val confirmCalling = booleanPreferencesKey("confirm_calling")
        val sentMessageSoundVolume = intPreferencesKey("sent_message_sound_volume")
        val sentMessageSoundUri = stringPreferencesKey("sent_message_sound_uri")
        val callSoundOrdinal = intPreferencesKey("call_sound")
        val callSoundVolume = intPreferencesKey("call_sound_volume")
        val callRingtoneUri = stringPreferencesKey("call_ringtone_uri")
        val notificationSoundVolume = intPreferencesKey("notification_sound_volume")
        val notificationSoundUri = stringPreferencesKey("notification_sound_uri")
        val activeChatSoundVolume = intPreferencesKey("active_chat_sound_volume")
        val activeChatSoundUri = stringPreferencesKey("active_chat_sound_uri")
        val hapticEnabled = booleanPreferencesKey("haptic_enabled")
        val autoSaveToDownloads = booleanPreferencesKey("auto_save_to_downloads")
    }
}

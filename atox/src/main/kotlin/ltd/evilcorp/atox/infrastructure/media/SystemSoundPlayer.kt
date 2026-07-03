package ltd.evilcorp.atox.infrastructure.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_VOLUME = 100
private const val MAX_VOLUME_FLOAT = 100f

@Singleton
class SystemSoundPlayer @Inject constructor(
    private val context: Context,
) {

    fun playNotificationSound(uriString: String, volume: Int) =
        playOneShot(uriString, RingtoneManager.TYPE_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION_EVENT, volume)

    fun playSentSound(uriString: String, volume: Int) =
        playOneShot(uriString, RingtoneManager.TYPE_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION_EVENT, volume)

    private fun playOneShot(uriString: String, type: Int, usage: Int, volume: Int) {
        val safeVolume = volume.coerceIn(0, MAX_VOLUME)
        if (safeVolume <= 0) return
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(type)
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val normalized = safeVolume / MAX_VOLUME_FLOAT
                setVolume(normalized, normalized)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
        }
    }
}

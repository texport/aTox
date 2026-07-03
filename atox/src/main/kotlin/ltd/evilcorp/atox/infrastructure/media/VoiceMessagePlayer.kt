package ltd.evilcorp.atox.infrastructure.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream

private const val TAG = "VoiceMessagePlayer"

object VoiceMessagePlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUri: String? = null

    fun play(context: Context, uriString: String, onComplete: () -> Unit, onError: () -> Unit) {
        try {
            stop()

            val player = MediaPlayer()
            mediaPlayer = player

            val parsedUri = Uri.parse(uriString)

            // For file:// URIs pointing to app-private directories, we must use
            // FileDescriptor because MediaPlayer.setDataSource(Context, Uri) with file:// URIs
            // delegates to the system mediaserver process which lacks read permission
            // on the app's private cache directory.
            when (parsedUri.scheme) {
                "content" -> {
                    player.setDataSource(context, parsedUri)
                }
                "file" -> {
                    val path = parsedUri.path
                    if (path != null) {
                        val fis = FileInputStream(File(path))
                        try {
                            player.setDataSource(fis.fd)
                        } finally {
                            fis.close()
                        }
                    } else {
                        player.setDataSource(context, parsedUri)
                    }
                }
                null -> {
                    val fis = FileInputStream(File(uriString))
                    try {
                        player.setDataSource(fis.fd)
                    } finally {
                        fis.close()
                    }
                }
                else -> {
                    player.setDataSource(context, parsedUri)
                }
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer native error: what=$what, extra=$extra")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    stop()
                    onError()
                }
                true
            }
            player.setOnCompletionListener {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    stop()
                    onComplete()
                }
            }

            player.prepare()
            player.start()
            currentPlayingUri = uriString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice message", e)
            stop()
            onError()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    @Suppress("unused")
    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        val player = mediaPlayer
        mediaPlayer = null
        currentPlayingUri = null
        if (player != null) {
            try {
                player.setOnErrorListener(null)
                player.setOnCompletionListener(null)
            } catch (_: Exception) {
                // Ignore
            }
            try {
                player.release()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    @Suppress("unused")
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun isPlayingUri(uriString: String): Boolean = currentPlayingUri == uriString && mediaPlayer != null
}

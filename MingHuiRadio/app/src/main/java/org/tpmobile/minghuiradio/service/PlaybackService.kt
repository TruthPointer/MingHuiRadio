package org.tpmobile.minghuiradio.service


import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.tpmobile.minghuiradio.exoplayer.util.ExoplayerUtil.getMediaSourceFactory

class PlaybackService : MediaSessionService() {
  private var exoPlayer: ExoPlayer? = null
  private var mediaSession: MediaSession? = null

  override fun onCreate() {
    super.onCreate()
    exoPlayer = ExoPlayer.Builder(this)
      .setMediaSourceFactory(getMediaSourceFactory(this))
      .build().also {
        mediaSession = MediaSession.Builder(this, it).build()
      }
  }

  override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  @OptIn(UnstableApi::class)
  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    pauseAllPlayersAndStopSelf()
  }

  override fun onDestroy() {
    exoPlayer?.run {
      stop()
      release()
    }
    exoPlayer = null
    mediaSession?.run {
      release()
    }
    mediaSession = null
    super.onDestroy()
  }
}
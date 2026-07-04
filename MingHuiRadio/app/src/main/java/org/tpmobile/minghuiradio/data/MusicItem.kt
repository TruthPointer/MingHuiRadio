package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import androidx.room.Ignore
import org.tpmobile.minghuiradio.util.FileUtil

@Keep
@Entity(tableName = "music_item", primaryKeys = ["title", "url"])
data class MusicItem(
  //@PrimaryKey
  val title: String,
  var url: String,
  val filePath: String = "",
  var duration: String = "",
  var date: String = "",
  var isFavorite: Boolean = false,
  var isPlaying: Boolean = false,
  //下载暂停的位置
  var downloadPercent: Int = 0,
  //播放暂停的位置
  var pausedPosition: Long = 0
) {
  /**
   * 保持获取时的顺序
   */
  @Ignore
  var index: Int = 0

  fun toMediaItem() = MediaItem.Builder()
    .setUri(url)
    .setMediaMetadata(
      MediaMetadata.Builder().setTitle(title).setArtworkUri(FileUtil.getOneUriForArtwork()).build()
    )
    .build()

  fun toFavoriteItem() = FavoriteItem(
    title, url, filePath, duration, date, false, downloadPercent, pausedPosition
  )
}
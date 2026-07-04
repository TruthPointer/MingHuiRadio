package org.tpmobile.minghuiradio.data

import androidx.annotation.Keep
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import org.tpmobile.minghuiradio.util.FileUtil

@Keep
@Entity(tableName = "favorite_item", primaryKeys = ["title", "url"])
data class FavoriteItem(
  //@PrimaryKey(autoGenerate = true)
  //val id: Int,
  //@PrimaryKey
  val title: String,
  var url: String,
  val filePath: String = "",
  var duration: String = "",
  var date: String = "",
  /**
   * 表示：被选择，处于播放状态
   */
  var isPlaying: Boolean = false,
  //下载暂停的位置
  var downloadPercent: Int = 0,
  //播放暂停的位置
  var pausedPosition: Long = 0
) {
  fun toMediaItem() = MediaItem.Builder()
    .setUri(url)
    .setMediaMetadata(
      MediaMetadata.Builder().setTitle(title).setArtworkUri(FileUtil.getOneUriForArtwork()).build()
    )
    .build()
}
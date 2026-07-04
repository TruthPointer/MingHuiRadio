package org.tpmobile.minghuiradio.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.tpmobile.minghuiradio.data.MusicItem

@Dao
interface MusicDao {
  @Query("SELECT * FROM music_item")
  fun getAll(): Flow<List<MusicItem>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(musicItems: List<MusicItem>)

  @Query("DELETE FROM music_item")
  suspend fun deleteAll()

  @Query("UPDATE music_item SET downloadPercent=:percent WHERE url =:url")
  suspend fun updateDownloadPercent(url: String, percent: Int)

  @Query("UPDATE music_item SET isPlaying=:isPlaying WHERE url =:url")
  suspend fun updatePlayState(url: String, isPlaying: Boolean)

  @Query("UPDATE music_item SET isFavorite=:isFavorite WHERE url =:url")
  suspend fun updateFavoriteState(url: String, isFavorite: Boolean)

  @Query("UPDATE music_item SET isPlaying = 0")
  suspend fun clearAllPlayState()
}
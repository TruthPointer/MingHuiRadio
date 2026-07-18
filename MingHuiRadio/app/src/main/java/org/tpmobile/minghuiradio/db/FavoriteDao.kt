package org.tpmobile.minghuiradio.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.tpmobile.minghuiradio.data.FavoriteItem

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorite_item")
  fun getAll(): Flow<List<FavoriteItem>>

  @Query("SELECT * FROM favorite_item")
  fun getAllWithoutFlow(): List<FavoriteItem>

  @Query("SELECT * FROM favorite_item")
  suspend fun getAllItems(): List<FavoriteItem>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(favoriteItems: List<FavoriteItem>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(favoriteItem: FavoriteItem)

  @Delete
  suspend fun delete(favoriteItem: FavoriteItem)

  @Query("DELETE FROM favorite_item")
  suspend fun deleteAll()

  @Query("UPDATE favorite_item SET downloadPercent=:percent WHERE url =:url")
  suspend fun updateDownloadPercent(url: String, percent: Int)

  @Query("UPDATE favorite_item SET isPlaying=:isPlaying WHERE url =:url")
  suspend fun updatePlayState(url: String, isPlaying: Boolean)

  @Query("UPDATE favorite_item SET isPlaying = 0")
  suspend fun clearAllPlayState()
}
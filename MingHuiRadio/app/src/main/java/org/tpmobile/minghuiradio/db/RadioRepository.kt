package org.tpmobile.minghuiradio.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.tpmobile.minghuiradio.data.FavoriteItem
import org.tpmobile.minghuiradio.data.MusicItem
import org.tpmobile.minghuiradio.data.WebSiteCategory
import org.tpmobile.minghuiradio.data.WebSiteInfo
import org.tpmobile.minghuiradio.util.Logger
import org.tpmobile.minghuiradio.util.ParseHelper
import org.tpmobile.minghuiradio.util.RESOURCE_MING_HUI_RADIO

class RadioRepository(
  private val siteCategoryDb: SiteCategoryDb,
  private val musicDb: MusicDb,
  private val favoriteDb: FavoriteDb,
) {

  private val TAG = "RadioRepository"

  companion object {
    private var INSTANCE: RadioRepository? = null

    fun getInstance(): RadioRepository {
      return INSTANCE ?: synchronized(this) {
        RadioRepository(
          SiteCategoryDb.getInstance(),
          MusicDb.getInstance(),
          FavoriteDb.getInstance()
        )
      }
    }
  }

  ////////////////////////////////////
  //
  ////////////////////////////////////
  suspend fun getSiteCategories(webSiteName: String): List<WebSiteCategory>? =
    withContext(Dispatchers.IO) {
      return@withContext siteCategoryDb.webSiteDao().getAllByWebSiteName(webSiteName)
    }

  suspend fun getAllSiteCategories(webSiteInfos: List<WebSiteInfo>): List<WebSiteCategory> =
    withContext(Dispatchers.IO) {
      val deferreds = webSiteInfos.map { info ->
        async {
          if (info.webSiteName == RESOURCE_MING_HUI_RADIO)
            ParseHelper.parseCategoryInfoForMHR(info, info.webSiteUrl)
          else
            ParseHelper.parseCategoryInfoForMHRInSOH(info, info.webSiteUrl)
        }
      }
      deferreds.awaitAll().forEach { result ->
        result.fold(
          onSuccess = { siteCategories ->
            siteCategoryDb.webSiteDao().insert(siteCategories)
          },
          onFailure = { e ->
            Logger.e(TAG, "[RadioRepository] getAllSiteCategories: 错误：${e.message}")
          }
        )
      }
      val newList = siteCategoryDb.webSiteDao().getAll()
      return@withContext newList
    }

  suspend fun insertSiteCategories(siteCategories: List<WebSiteCategory>) =
    withContext(Dispatchers.IO) {
      siteCategoryDb.webSiteDao().insert(siteCategories)
    }

  ////////////////////////////////////
  //
  ////////////////////////////////////
  suspend fun getAllMusicItems(): Flow<List<MusicItem>> = withContext(Dispatchers.IO) {
    musicDb.musicDao().getAll()
  }

  suspend fun insertMusicItems(musicItems: List<MusicItem>) = withContext(Dispatchers.IO) {
    musicDb.musicDao().deleteAll()
    musicDb.musicDao().insertAll(musicItems)
  }

  suspend fun updateMusicItemPlayState(url: String, isPlaying: Boolean) {
    musicDb.musicDao().updatePlayState(url, isPlaying)
  }

  suspend fun updateMusicItemFavorite(url: String, isFavorite: Boolean) {
    musicDb.musicDao().updateFavoriteState(url, isFavorite)
  }

  suspend fun clearAllMusicItemPlayState() {
    musicDb.musicDao().clearAllPlayState()
  }

  ////////////////////////////////////
  //
  ////////////////////////////////////
  suspend fun getAllFavoriteItems(): Flow<List<FavoriteItem>> = withContext(Dispatchers.IO) {
    favoriteDb.favoriteDao().getAll()
  }

  suspend fun getAllFavoriteItemsWithoutFlow(): List<FavoriteItem> = withContext(Dispatchers.IO) {
    favoriteDb.favoriteDao().getAllWithoutFlow()
  }

  suspend fun insertFavoriteItem(favoriteItem: FavoriteItem) = withContext(Dispatchers.IO) {
    favoriteDb.favoriteDao().insert(favoriteItem)
  }

  suspend fun deleteFavoriteItem(favoriteItem: FavoriteItem) = withContext(Dispatchers.IO) {
    favoriteDb.favoriteDao().delete(favoriteItem)
  }

  suspend fun clearAllFavoriteItems() = withContext(Dispatchers.IO) {
    favoriteDb.favoriteDao().getAllItems().map { it.url }.forEach {
      musicDb.musicDao().updateFavoriteState(it, false)
      musicDb.musicDao().updatePlayState(it, false)
    }
    favoriteDb.favoriteDao().deleteAll()
  }

  suspend fun updateFavoriteItemPlayState(url: String, isPlaying: Boolean) {
    favoriteDb.favoriteDao().updatePlayState(url, isPlaying)
  }

  suspend fun clearAllFavoriteItemPlayState() {
    favoriteDb.favoriteDao().clearAllPlayState()
  }

}

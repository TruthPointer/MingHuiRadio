package org.tpmobile.minghuiradio.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.tpmobile.minghuiradio.data.WebSiteCategory

@Dao
interface SiteCategoryDao {
  @Query("SELECT * FROM site_category")
  suspend fun getAll(): List<WebSiteCategory>

  @Query("SELECT * FROM site_category WHERE web_site_name = :webSiteName")
  suspend fun getAllByWebSiteName(webSiteName: String): List<WebSiteCategory>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(siteCategories: List<WebSiteCategory>)

  @Query("DELETE FROM site_category WHERE web_site_name = :webSiteName")
  suspend fun deleteAllByWebSiteName(webSiteName: String)

  @Query("DELETE FROM site_category")
  suspend fun deleteAll()


}
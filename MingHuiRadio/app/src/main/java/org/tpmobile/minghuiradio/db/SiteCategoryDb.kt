package org.tpmobile.minghuiradio.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.data.WebSiteCategory

@Database(
  entities = [
    WebSiteCategory::class,
  ], version = 1, exportSchema = false
)
@TypeConverters(MyTypeConverters::class)
abstract class SiteCategoryDb : RoomDatabase() {
  companion object {
    private const val DATABASE_NAME = "web_site.db"
    private var INSTANCE: SiteCategoryDb? = null

    fun getInstance(): SiteCategoryDb {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          MyApp.appContext,
          SiteCategoryDb::class.java,
          DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
        INSTANCE = instance
        instance
      }
    }
  }

  abstract fun webSiteDao(): SiteCategoryDao

}

package org.tpmobile.minghuiradio.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.data.FavoriteItem

@Database(entities = [FavoriteItem::class], version = 2, exportSchema = false)
abstract class FavoriteDb : RoomDatabase() {
  companion object {
    private const val DATABASE_NAME = "favorite_item.db"
    private var INSTANCE: FavoriteDb? = null

    fun getInstance(): FavoriteDb {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          MyApp.appContext,
          FavoriteDb::class.java,
          DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
        INSTANCE = instance
        instance
      }
    }
  }

  abstract fun favoriteDao(): FavoriteDao
}
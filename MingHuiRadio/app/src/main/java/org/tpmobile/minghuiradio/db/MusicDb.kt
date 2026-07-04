package org.tpmobile.minghuiradio.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tpmobile.minghuiradio.MyApp
import org.tpmobile.minghuiradio.data.MusicItem

@Database(entities = [MusicItem::class], version = 2, exportSchema = false)
abstract class MusicDb : RoomDatabase() {
  companion object {
    private const val DATABASE_NAME = "music_item.db"
    private var INSTANCE: MusicDb? = null

    fun getInstance(): MusicDb {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          MyApp.appContext,
          MusicDb::class.java,
          DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
        INSTANCE = instance
        instance
      }
    }
  }

  abstract fun musicDao(): MusicDao

}
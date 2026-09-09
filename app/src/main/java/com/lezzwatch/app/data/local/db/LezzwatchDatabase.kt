package com.lezzwatch.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class LezzwatchDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var instance: LezzwatchDatabase? = null

        fun getInstance(context: Context): LezzwatchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LezzwatchDatabase::class.java,
                    "lezzwatch.db",
                ).build().also { instance = it }
            }
    }
}

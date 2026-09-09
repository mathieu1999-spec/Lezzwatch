package com.lezzwatch.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoriteEntity::class, HiddenChannelEntity::class], version = 2, exportSchema = false)
abstract class LezzwatchDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenChannelDao(): HiddenChannelDao

    companion object {
        @Volatile private var instance: LezzwatchDatabase? = null

        /** Adds the hidden-channels table introduced in version 2 without touching the existing
         * favorites table, so upgrading the app doesn't wipe anyone's saved favorites. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hidden_channels` (" +
                        "`channelId` TEXT NOT NULL, `hiddenAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`channelId`))",
                )
            }
        }

        fun getInstance(context: Context): LezzwatchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LezzwatchDatabase::class.java,
                    "lezzwatch.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}

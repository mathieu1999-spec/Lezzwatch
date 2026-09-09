package com.lezzwatch.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenChannelDao {

    @Query("SELECT * FROM hidden_channels")
    fun observeHidden(): Flow<List<HiddenChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(hidden: HiddenChannelEntity)

    @Query("DELETE FROM hidden_channels WHERE channelId = :channelId")
    suspend fun remove(channelId: String)
}

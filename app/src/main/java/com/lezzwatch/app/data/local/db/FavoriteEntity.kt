package com.lezzwatch.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A favorited channel, keyed by [Channel.id][com.lezzwatch.app.data.model.Channel.id]. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val addedAtEpochMillis: Long,
)

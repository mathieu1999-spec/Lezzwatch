package com.lezzwatch.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A hidden channel, keyed by [Channel.id][com.lezzwatch.app.data.model.Channel.id]. Hidden
 * channels are excluded from browsing, search, and every channel picker in the app except the
 * Settings "Hidden Channels" screen, which manages this table directly. */
@Entity(tableName = "hidden_channels")
data class HiddenChannelEntity(
    @PrimaryKey val channelId: String,
    val hiddenAtEpochMillis: Long,
)

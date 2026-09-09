package com.lezzwatch.app.data.repository

import com.lezzwatch.app.data.local.db.FavoriteDao
import com.lezzwatch.app.data.local.db.FavoriteEntity
import com.lezzwatch.app.data.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for channel data: loads the playlist (once, lazily) via
 * [PlaylistSource] and continuously stitches in favorite status from Room, so every screen
 * observing [channels] automatically reflects favorite toggles anywhere else in the app.
 *
 * This class is an app-scoped singleton (see [com.lezzwatch.app.di.AppContainer]) so the
 * playlist is parsed exactly once per process lifetime, not once per screen.
 */
class ChannelRepository(
    private val playlistSource: PlaylistSource,
    private val favoriteDao: FavoriteDao,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadMutex = Mutex()

    private val rawChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val loaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = loaded

    private val favoriteIds: StateFlow<Set<String>> = favoriteDao.observeFavorites()
        .map { entities -> entities.map(FavoriteEntity::channelId).toSet() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptySet())

    /** All channels, each with an up-to-date [Channel.isFavorite] flag. */
    val channels: StateFlow<List<Channel>> = combine(rawChannels, favoriteIds) { raw, favIds ->
        raw.map { it.copy(isFavorite = it.id in favIds) }
    }.stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = channels.map { list -> list.filter { it.isFavorite } }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    /** Parses the bundled playlist on first call; subsequent calls are no-ops. Safe to call from
     * multiple screens concurrently on app start. */
    suspend fun ensureLoaded() {
        if (loaded.value) return
        loadMutex.withLock {
            if (loaded.value) return
            rawChannels.value = playlistSource.loadChannels()
            loaded.value = true
        }
    }

    fun availableCountries(): List<String> =
        channels.value.map { it.country }.distinct().sortedWith(countryComparator())

    fun availableGenres(): List<String> =
        channels.value.map { it.genre }.distinct().sorted()

    suspend fun toggleFavorite(channel: Channel) {
        if (channel.isFavorite) {
            favoriteDao.remove(channel.id)
        } else {
            favoriteDao.add(FavoriteEntity(channel.id, System.currentTimeMillis()))
        }
    }

    suspend fun clearFavorites() = favoriteDao.clearAll()

    fun findById(channelId: String): Channel? = channels.value.firstOrNull { it.id == channelId }

    /** Keeps "International" (the unknown-country fallback) at the end of any country list. */
    private fun countryComparator(): Comparator<String> = Comparator { a, b ->
        when {
            a == Channel.UNKNOWN_COUNTRY && b != Channel.UNKNOWN_COUNTRY -> 1
            b == Channel.UNKNOWN_COUNTRY && a != Channel.UNKNOWN_COUNTRY -> -1
            else -> a.compareTo(b)
        }
    }
}

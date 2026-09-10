package com.lezzwatch.app.data.repository

import com.lezzwatch.app.data.local.db.FavoriteDao
import com.lezzwatch.app.data.local.db.FavoriteEntity
import com.lezzwatch.app.data.local.db.HiddenChannelDao
import com.lezzwatch.app.data.local.db.HiddenChannelEntity
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
 * [PlaylistSource] and continuously stitches in favorite/hidden status from Room, so every screen
 * observing [channels] automatically reflects favorite/hide toggles anywhere else in the app.
 *
 * This class is an app-scoped singleton (see [com.lezzwatch.app.di.AppContainer]) so the
 * playlist is parsed exactly once per process lifetime, not once per screen.
 */
class ChannelRepository(
    private val playlistSource: PlaylistSource,
    private val favoriteDao: FavoriteDao,
    private val hiddenChannelDao: HiddenChannelDao,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadMutex = Mutex()

    private val rawChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val loaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = loaded

    private val favoriteIds: StateFlow<Set<String>> = favoriteDao.observeFavorites()
        .map { entities -> entities.map(FavoriteEntity::channelId).toSet() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptySet())

    private val hiddenIds: StateFlow<Set<String>> = hiddenChannelDao.observeHidden()
        .map { entities -> entities.map(HiddenChannelEntity::channelId).toSet() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptySet())

    /** Every channel from the playlist, each with up-to-date [Channel.isFavorite] and
     * [Channel.isHidden] flags. Includes hidden channels — this is the list the Settings
     * "Hidden Channels" screen manages. Everywhere else (browsing, search, the in-player channel
     * switcher, favorites) should use [visibleChannels] instead. */
    val channels: StateFlow<List<Channel>> = combine(rawChannels, favoriteIds, hiddenIds) { raw, favIds, hidden ->
        raw.map { it.copy(isFavorite = it.id in favIds, isHidden = it.id in hidden) }
    }.stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    /** [channels] with hidden ones filtered out. */
    val visibleChannels: StateFlow<List<Channel>> = channels
        .map { list -> list.filterNot { it.isHidden } }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = visibleChannels.map { list -> list.filter { it.isFavorite } }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    /** Parses the playlist on first call; subsequent calls are no-ops. Safe to call from multiple
     * screens concurrently on app start. */
    suspend fun ensureLoaded() {
        if (loaded.value) return
        loadMutex.withLock {
            if (loaded.value) return
            loadInternal()
        }
    }

    /** Re-parses the playlist from [playlistSource] regardless of whether it was already loaded —
     * used after Advanced Settings changes which playlist is active, since that's a runtime
     * preference change rather than something the app only reads once at startup. */
    suspend fun reload() {
        loadMutex.withLock { loadInternal() }
    }

    private suspend fun loadInternal() {
        rawChannels.value = playlistSource.loadChannels()
        loaded.value = true
    }

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

    suspend fun hideChannel(channel: Channel) {
        hiddenChannelDao.add(HiddenChannelEntity(channel.id, System.currentTimeMillis()))
    }

    suspend fun unhideChannel(channel: Channel) {
        hiddenChannelDao.remove(channel.id)
    }

    fun findById(channelId: String): Channel? = channels.value.firstOrNull { it.id == channelId }
}

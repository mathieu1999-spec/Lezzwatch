package com.lezzwatch.app.di

import android.content.Context
import com.lezzwatch.app.data.local.db.LezzwatchDatabase
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.data.repository.ConfigurablePlaylistSource
import com.lezzwatch.app.data.repository.EpgRepository
import com.lezzwatch.app.data.repository.PlaylistFileStore

/**
 * Small hand-rolled dependency container. The app is intentionally small enough that a full DI
 * framework (Hilt/Koin) would be overhead without real benefit — this single object wires up the
 * app-scoped singletons (repository, database, preferences) once and hands them to
 * ViewModels via their factories. See [com.lezzwatch.app.LezzwatchApplication].
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database by lazy { LezzwatchDatabase.getInstance(appContext) }

    val userPreferencesRepository by lazy { UserPreferencesRepository(appContext) }

    val playlistFileStore by lazy { PlaylistFileStore(appContext) }

    private val playlistSource by lazy {
        ConfigurablePlaylistSource(appContext, userPreferencesRepository, playlistFileStore)
    }

    val channelRepository by lazy {
        ChannelRepository(
            playlistSource = playlistSource,
            favoriteDao = database.favoriteDao(),
            hiddenChannelDao = database.hiddenChannelDao(),
        )
    }

    val epgRepository by lazy { EpgRepository(playlistSource) }
}

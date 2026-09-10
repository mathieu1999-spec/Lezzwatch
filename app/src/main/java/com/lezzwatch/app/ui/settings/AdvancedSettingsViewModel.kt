package com.lezzwatch.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lezzwatch.app.data.local.prefs.PlaylistMode
import com.lezzwatch.app.data.local.prefs.UserPreferences
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.data.repository.EpgRepository
import com.lezzwatch.app.data.repository.PlaylistFileStore
import com.lezzwatch.app.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdvancedSettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val playlistFileStore: PlaylistFileStore,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /** Drops the bundled playlist without replacing it — [ChannelRepository.channels] goes empty
     * until the user imports a custom one or restores the bundled default. */
    fun removeBundledPlaylist() {
        viewModelScope.launch {
            preferencesRepository.setPlaylistMode(PlaylistMode.REMOVED)
            reloadPlaylist()
        }
    }

    fun restoreBundledPlaylist() {
        viewModelScope.launch {
            preferencesRepository.setPlaylistMode(PlaylistMode.BUNDLED)
            reloadPlaylist()
        }
    }

    /** Copies [uri] into app storage and switches to it. [onError] fires on a read failure (e.g.
     * the picked file disappeared or isn't readable) so the UI can show a message — the playlist
     * mode is only flipped after the copy succeeds, so a failed import can't leave the app
     * pointed at a file that was never actually written. */
    fun importPlaylist(uri: Uri, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                playlistFileStore.import(uri)
                preferencesRepository.setPlaylistMode(PlaylistMode.CUSTOM)
                reloadPlaylist()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    private suspend fun reloadPlaylist() {
        channelRepository.reload()
        epgRepository.reload()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AdvancedSettingsViewModel(
                    preferencesRepository = container.userPreferencesRepository,
                    playlistFileStore = container.playlistFileStore,
                    channelRepository = container.channelRepository,
                    epgRepository = container.epgRepository,
                )
            }
        }
    }
}

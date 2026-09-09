package com.lezzwatch.app.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.di.appContainer
import com.lezzwatch.app.player.cast.CastPlayerController
import com.lezzwatch.app.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerScreenState(
    val channel: Channel? = null,
    val playbackState: PlaybackUiState = PlaybackUiState.Loading,
    val isCasting: Boolean = false,
    val isCastAvailable: Boolean = false,
)

/**
 * Owns the ExoPlayer (and, when available, the CastPlayer) instance for the lifetime of
 * [com.lezzwatch.app.player.PlayerActivity]. Kept as a ViewModel rather than an Activity field
 * so it survives the activity's own `onConfigChanged`-driven re-layouts (entering/exiting PiP,
 * rotating) without ever tearing down and rebuilding the player — that would cause a visible
 * re-buffer every time.
 */
class PlayerViewModel(
    context: Context,
    initialChannelId: String,
    private val channelRepository: ChannelRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val appContext = context.applicationContext

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        playWhenReady = true
    }

    private val castController = CastPlayerController(appContext)

    private val _activePlayer = MutableStateFlow<Player>(exoPlayer)
    val activePlayer: StateFlow<Player> = _activePlayer

    private val _currentChannelId = MutableStateFlow(initialChannelId)
    private val _playbackState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)

    val uiState: StateFlow<PlayerScreenState> = combine(
        channelRepository.channels,
        _currentChannelId,
        _playbackState,
        castController.isCasting,
    ) { channels, channelId, playback, isCasting ->
        PlayerScreenState(
            channel = channels.firstOrNull { it.id == channelId },
            playbackState = playback,
            isCasting = isCasting,
            isCastAvailable = castController.isCastAvailable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerScreenState())

    /** Full channel list (with live favorite flags) for the in-player channel-switcher sheet. */
    val allChannels: StateFlow<List<Channel>> = channelRepository.channels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        exoPlayer.addListener(playerStateListener(exoPlayer))
        castController.castPlayer?.addListener(playerStateListener(castController.castPlayer!!))
        castController.setSessionAvailabilityListener(
            onSessionAvailable = { switchToCast() },
            onSessionUnavailable = { switchToLocal() },
        )
        viewModelScope.launch {
            channelRepository.ensureLoaded()
            playChannel(initialChannelId)
        }
    }

    fun selectChannel(channelId: String) {
        if (channelId == _currentChannelId.value) return
        _currentChannelId.value = channelId
        playChannel(channelId)
    }

    fun retry() = playChannel(_currentChannelId.value)

    fun toggleFavorite() {
        uiState.value.channel?.let(::toggleFavoriteFor)
    }

    /** Toggle favorite for an arbitrary channel (used by the in-player channel drawer, where the
     * tapped row isn't necessarily the channel currently playing). */
    fun toggleFavoriteFor(channel: Channel) {
        viewModelScope.launch { channelRepository.toggleFavorite(channel) }
    }

    /** Exposed so the Compose UI can attach Google's MediaRouteButton, which needs the
     * CastContext directly rather than going through our StateFlow-based state. */
    fun castContextOrNull() = castController.castContext

    private fun playChannel(channelId: String) {
        val channel = channelRepository.findById(channelId)
        if (channel == null) {
            _playbackState.value = PlaybackUiState.Error("Channel not found.")
            return
        }

        if (!NetworkMonitor.isOnline(appContext)) {
            _playbackState.value = PlaybackUiState.NoNetwork
            return
        }

        _playbackState.value = PlaybackUiState.Loading

        val mediaItem = MediaItem.Builder()
            .setUri(channel.streamUrl)
            .setMediaId(channel.id)
            // The bundled/expected playlist format is HLS; declaring the MIME type explicitly
            // (rather than relying on URL-extension sniffing) makes both local ExoPlayer
            // playback and Cast's media-info conversion more reliable for stream URLs that
            // don't end in ".m3u8" (query strings, "smil:" paths, etc. — common in the wild).
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(channel.name)
                    .apply {
                        channel.logoUrl?.let { setArtworkUri(Uri.parse(it)) }
                    }
                    .build(),
            )
            .build()

        _activePlayer.value.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }

        viewModelScope.launch { preferencesRepository.setLastWatchedChannelId(channel.id) }
    }

    private fun switchToCast() {
        val castPlayer = castController.castPlayer ?: return
        exoPlayer.pause()
        _activePlayer.value = castPlayer
        playChannel(_currentChannelId.value)
    }

    private fun switchToLocal() {
        _activePlayer.value = exoPlayer
        playChannel(_currentChannelId.value)
    }

    /** Both exoPlayer and (when available) castPlayer get their own instance of this listener,
     * each gated on actually being the currently active player — otherwise a background event
     * from the player we just switched away from could stomp on fresh state from the new one. */
    private fun playerStateListener(forPlayer: Player) = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (_activePlayer.value !== forPlayer) return
            when (playbackState) {
                Player.STATE_BUFFERING -> _playbackState.value = PlaybackUiState.Loading
                Player.STATE_READY -> _playbackState.value = PlaybackUiState.Ready
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (_activePlayer.value !== forPlayer) return
            _playbackState.value = if (!NetworkMonitor.isOnline(appContext)) {
                PlaybackUiState.NoNetwork
            } else {
                PlaybackUiState.Error(error.errorCodeName)
            }
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        castController.release()
    }

    companion object {
        fun factory(context: Context, channelId: String) = viewModelFactory {
            initializer {
                val container = appContainer()
                PlayerViewModel(context, channelId, container.channelRepository, container.userPreferencesRepository)
            }
        }
    }
}

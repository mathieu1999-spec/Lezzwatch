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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.model.EpgProgramme
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.data.repository.EpgRepository
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.di.appContainer
import com.lezzwatch.app.player.cast.CastPlayerController
import com.lezzwatch.app.player.recording.StreamRecorder
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
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Int = 0,
)

/** Bundles the recorder's two flows so they can be folded into the 5-flow [combine] below
 * (kotlinx.coroutines' typed `combine` tops out at 5 flows). */
private data class RecordingUiState(val isRecording: Boolean, val elapsedSeconds: Int)

/** A brief "what's on" glance to show right after switching to [channelId] — see
 * [PlayerViewModel.epgGlance]. Either programme may be null when the guide has no data for this
 * channel at all, or nothing airing after the current slot. */
data class EpgGlance(val channelId: String, val current: EpgProgramme?, val next: EpgProgramme?)

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
    private val epgRepository: EpgRepository,
) : ViewModel() {

    private val appContext = context.applicationContext

    private val streamRecorder = StreamRecorder(appContext)

    /** Every HTTP read ExoPlayer performs (manifest + segment fetches) is routed through
     * [streamRecorder]'s wrapper so it can mirror media-segment bytes to a local file while a
     * recording is active — see [StreamRecorder]. Only applies to local playback: casting sends
     * the URL to the receiver device directly, so there's no local byte stream to capture. */
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(appContext, streamRecorder.wrap(DefaultHttpDataSource.Factory())),
            ),
        )
        .build()
        .apply { playWhenReady = true }

    private val castController = CastPlayerController(appContext)

    private val _activePlayer = MutableStateFlow<Player>(exoPlayer)
    val activePlayer: StateFlow<Player> = _activePlayer

    private val _currentChannelId = MutableStateFlow(initialChannelId)
    private val _playbackState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Loading)

    private val recordingState = combine(
        streamRecorder.isRecording,
        streamRecorder.elapsedSeconds,
    ) { isRecording, elapsed -> RecordingUiState(isRecording, elapsed) }

    val uiState: StateFlow<PlayerScreenState> = combine(
        channelRepository.channels,
        _currentChannelId,
        _playbackState,
        castController.isCasting,
        recordingState,
    ) { channels, channelId, playback, isCasting, recording ->
        PlayerScreenState(
            channel = channels.firstOrNull { it.id == channelId },
            playbackState = playback,
            isCasting = isCasting,
            isCastAvailable = castController.isCastAvailable,
            isRecording = recording.isRecording,
            recordingElapsedSeconds = recording.elapsedSeconds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerScreenState())

    /** Full channel list (with live favorite flags) for the in-player channel-switcher sheet. */
    val allChannels: StateFlow<List<Channel>> = channelRepository.channels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _epgGlance = MutableStateFlow<EpgGlance?>(null)

    /** Emits once per channel switch, when guide data for that channel becomes available — the
     * UI shows it as a brief overlay and clears it after a few seconds. Null means either nothing
     * has arrived yet for the current channel or the guide has no data for it. */
    val epgGlance: StateFlow<EpgGlance?> = _epgGlance

    init {
        exoPlayer.addListener(playerStateListener(exoPlayer))
        castController.castPlayer?.addListener(playerStateListener(castController.castPlayer!!))
        castController.setSessionAvailabilityListener(
            onSessionAvailable = { switchToCast() },
            onSessionUnavailable = { switchToLocal() },
        )
        viewModelScope.launch { epgRepository.ensureLoaded() }
        viewModelScope.launch {
            channelRepository.ensureLoaded()
            playChannel(initialChannelId)
        }
    }

    fun selectChannel(channelId: String) {
        if (channelId == _currentChannelId.value) return
        // A recording only makes sense for a single, continuous channel — switching channels
        // mid-recording would otherwise silently splice two unrelated streams into one file.
        streamRecorder.stop()
        _currentChannelId.value = channelId
        playChannel(channelId)
    }

    fun retry() = playChannel(_currentChannelId.value)

    /** Starts recording the current channel's stream to a local video file. No-ops while casting
     * (there's no local byte stream to capture) or if a recording is already running. Any
     * storage permission needed on older Android versions must be granted by the caller first. */
    fun startRecording() {
        val channel = uiState.value.channel ?: return
        if (uiState.value.isCasting) return
        streamRecorder.start(channel.name)
    }

    fun stopRecording() = streamRecorder.stop()

    /** TV-remote-style channel up/down: cycles through the user's favorite channels only (not
     * the full channel list), wrapping around at either end. No-ops when there are no favorites,
     * or advances to the first/last favorite when the current channel isn't one itself. */
    fun nextFavoriteChannel() = cycleFavoriteChannel(step = 1)

    fun previousFavoriteChannel() = cycleFavoriteChannel(step = -1)

    private fun cycleFavoriteChannel(step: Int) {
        val favorites = allChannels.value.filter { it.isFavorite }
        if (favorites.isEmpty()) return
        val currentIndex = favorites.indexOfFirst { it.id == _currentChannelId.value }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + step).mod(favorites.size)
        selectChannel(favorites[nextIndex].id)
    }

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
        // Clear any stale glance from the previous channel immediately; the real one (if the
        // guide has data for this channel) arrives once ensureLoaded()'s fetch has completed.
        _epgGlance.value = null
        viewModelScope.launch {
            epgRepository.ensureLoaded()
            val (current, next) = epgRepository.currentAndNext(channel.id)
            _epgGlance.value = if (current != null || next != null) {
                EpgGlance(channel.id, current, next)
            } else {
                null
            }
        }

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
        // Casting hands the stream URL to the receiver device, which fetches it independently —
        // our local data source stops seeing bytes, so a recording in progress can't continue.
        streamRecorder.stop()
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
        streamRecorder.release()
        exoPlayer.release()
        castController.release()
    }

    companion object {
        fun factory(context: Context, channelId: String) = viewModelFactory {
            initializer {
                val container = appContainer()
                PlayerViewModel(
                    context,
                    channelId,
                    container.channelRepository,
                    container.userPreferencesRepository,
                    container.epgRepository,
                )
            }
        }
    }
}

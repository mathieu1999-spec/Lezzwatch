package com.lezzwatch.app.player

/** Playback status for the currently selected channel. Kept separate from Media3's own player
 * states because we want simpler, product-facing states (Loading/Ready/Error/NoNetwork) rather
 * than exposing ExoPlayer's STATE_* constants directly to the UI layer. */
sealed interface PlaybackUiState {
    data object Loading : PlaybackUiState
    data object Ready : PlaybackUiState
    data object NoNetwork : PlaybackUiState
    data class Error(val message: String) : PlaybackUiState
}

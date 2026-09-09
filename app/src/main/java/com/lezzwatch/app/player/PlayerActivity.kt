package com.lezzwatch.app.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lezzwatch.app.data.local.prefs.AppTheme
import com.lezzwatch.app.ui.theme.LezzwatchTheme

/**
 * Dedicated full-screen player activity (see requirement 12: "the player ... should not feel
 * constrained by the bottom navigation"). Kept separate from [com.lezzwatch.app.MainActivity] so
 * Picture-in-Picture, orientation handling, and the immersive video surface all have a clean,
 * isolated Activity lifecycle instead of fighting with the bottom-nav host.
 */
class PlayerActivity : ComponentActivity() {

    private val channelId: String? by lazy { intent.getStringExtra(EXTRA_CHANNEL_ID) }

    private val playerViewModel: PlayerViewModel by viewModels {
        PlayerViewModel.factory(applicationContext, channelId.orEmpty())
    }

    private var isInPipModeState = mutableStateOf(false)
    private var wasPlayingBeforeStop = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (channelId.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            val isInPipMode by isInPipModeState

            // The player is always shown dark regardless of the user's app-wide theme setting —
            // it's a full-bleed video surface, light chrome around it would be jarring.
            LezzwatchTheme(appTheme = AppTheme.DARK) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    isInPipMode = isInPipMode,
                    onBack = { if (!isInPipMode) finish() },
                    onEnterPip = { enterPip() },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Don't pause when we're the ones putting ourselves into PiP — that's still "playing" as
        // far as the user is concerned. Otherwise, free up the network/CPU once fully backgrounded.
        if (!isInPipModeState.value) {
            wasPlayingBeforeStop = playerViewModel.activePlayer.value.isPlaying
            playerViewModel.activePlayer.value.pause()
        }
    }

    override fun onStart() {
        super.onStart()
        if (wasPlayingBeforeStop) {
            playerViewModel.activePlayer.value.play()
        }
    }

    /** Picture-in-Picture is entered automatically when the user navigates away while playing
     * (requirement 9) and can also be triggered explicitly from the in-player PiP button. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerViewModel.activePlayer.value.isPlaying) {
            enterPip()
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        runCatching { enterPictureInPictureMode(params) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    companion object {
        private const val EXTRA_CHANNEL_ID = "extra_channel_id"

        fun newIntent(context: Context, channelId: String): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ID, channelId)
            }
    }
}

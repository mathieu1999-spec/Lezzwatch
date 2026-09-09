package com.lezzwatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lezzwatch.app.data.local.prefs.UserPreferences
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.player.PlayerActivity
import com.lezzwatch.app.ui.navigation.LezzwatchNavHost
import com.lezzwatch.app.ui.theme.LezzwatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LezzwatchApplication).container

        setContent {
            // UserPreferences() (dark theme, no autoplay) is exactly what the repository itself
            // resolves to before any value has ever been written, so it's a safe initial value —
            // no first-frame null-handling needed, and no light/dark flash on cold start.
            val preferences by container.userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            LezzwatchTheme(appTheme = preferences.theme) {
                var didAttemptAutoplay by remember { mutableStateOf(false) }
                val isLoaded by container.channelRepository.isLoaded.collectAsStateWithLifecycle()

                LaunchedEffect(isLoaded, preferences, didAttemptAutoplay) {
                    if (didAttemptAutoplay || !isLoaded) return@LaunchedEffect
                    didAttemptAutoplay = true
                    val lastChannelId = preferences.lastWatchedChannelId
                    if (preferences.autoPlayLastChannel && lastChannelId != null) {
                        container.channelRepository.findById(lastChannelId)?.let(::openPlayer)
                    }
                }

                LezzwatchNavHost(onChannelSelected = ::openPlayer)
            }
        }
    }

    private fun openPlayer(channel: Channel) {
        startActivity(PlayerActivity.newIntent(this, channel.id))
    }
}

package com.lezzwatch.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.model.EpgProgramme
import com.lezzwatch.app.ui.theme.ErrorRed
import com.lezzwatch.app.ui.theme.FavoriteRed
import java.text.SimpleDateFormat
import java.util.Locale

/** Top overlay: back, channel name/country, record, favorite, PiP. Cast button is injected
 * separately (it wraps a platform AndroidView) — see [PlayerScreen]. */
@Composable
fun PlayerTopBar(
    channel: Channel?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEnterPip: () -> Unit,
    castButton: @Composable () -> Unit,
    isRecording: Boolean,
    recordingElapsedSeconds: Int,
    canRecord: Boolean,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GradientScrim)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Color.White)
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = channel?.name ?: "",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (channel != null) {
                Text(
                    text = "${channel.country} · ${channel.genre}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        castButton()

        if (isRecording) {
            Text(
                text = formatRecordingElapsed(recordingElapsedSeconds),
                color = ErrorRed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Deliberately the same icon whether idle or recording — only its tint (and the elapsed
        // timer above) change, so tapping it always reads as "toggle recording" rather than two
        // different actions.
        IconButton(onClick = onToggleRecording, enabled = canRecord) {
            Icon(
                imageVector = Icons.Filled.FiberManualRecord,
                contentDescription = stringResource(
                    if (isRecording) R.string.player_stop_recording else R.string.player_start_recording,
                ),
                tint = when {
                    isRecording -> ErrorRed
                    canRecord -> Color.White
                    else -> Color.White.copy(alpha = 0.3f)
                },
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (channel?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (channel?.isFavorite == true) R.string.player_unfavorite else R.string.player_favorite,
                ),
                tint = if (channel?.isFavorite == true) FavoriteRed else Color.White,
            )
        }

        IconButton(onClick = onEnterPip) {
            Icon(Icons.Filled.PictureInPicture, contentDescription = stringResource(R.string.player_pip), tint = Color.White)
        }
    }
}

/** Center tap-to-toggle play/pause button, only shown while controls are visible. */
@Composable
fun PlayPauseButton(isPlaying: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(64.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
    }
}

/** Bottom overlay: TV-remote-style channel up/down cluster (cycles through favorites) on the
 * left, button to open the channel-switcher sheet on the right. */
@Composable
fun PlayerBottomBar(
    onOpenChannelList: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    channelUpDownEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GradientScrimBottom)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelUpDownControls(
            enabled = channelUpDownEnabled,
            onChannelUp = onChannelUp,
            onChannelDown = onChannelDown,
        )

        androidx.compose.material3.FilledTonalButton(onClick = onOpenChannelList) {
            Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.player_channel_list))
        }
    }
}

/** Two stacked chevrons resembling a TV remote's channel up/down rocker. Cycles through the
 * user's favorite channels only — disabled (dimmed) when there are fewer than two to switch
 * between. */
@Composable
private fun ChannelUpDownControls(
    enabled: Boolean,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onChannelUp,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.3f),
            ),
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.player_channel_up),
            )
        }
        IconButton(
            onClick = onChannelDown,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.3f),
            ),
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.player_channel_down),
            )
        }
    }
}

/** Small, always-on-screen padlock (independent of the auto-hiding top/bottom bars — it has to
 * stay reachable even while everything else is locked away) that toggles whether touch controls
 * respond at all. Same-shaped button in both states; only the icon glyph and its content
 * description flip between locked/unlocked. */
@Composable
fun LockButton(locked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Icon(
            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = stringResource(
                if (locked) R.string.player_unlock_controls else R.string.player_lock_controls,
            ),
        )
    }
}

/** Brief "what's on" overlay shown for a few seconds right after switching channels — see
 * [PlayerViewModel.epgGlance]. */
@Composable
fun EpgGlanceOverlay(current: EpgProgramme?, next: EpgProgramme?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (current != null) {
            Text(
                text = "${formatTimeRange(current)}  ${current.title}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (next != null) {
            Text(
                text = stringResource(R.string.player_epg_next, next.title),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val timeOfDayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTimeRange(programme: EpgProgramme): String =
    "${timeOfDayFormat.format(programme.startMillis)}–${timeOfDayFormat.format(programme.stopMillis)}"

private fun formatRecordingElapsed(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private val GradientScrim = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
)

private val GradientScrimBottom = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
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
import com.lezzwatch.app.ui.theme.FavoriteRed

/** Top overlay: back, channel name/country, favorite, PiP. Cast button is injected separately
 * (it wraps a platform AndroidView) — see [PlayerScreen]. */
@Composable
fun PlayerTopBar(
    channel: Channel?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEnterPip: () -> Unit,
    castButton: @Composable () -> Unit,
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

/** Bottom overlay: button to open the channel-switcher sheet. */
@Composable
fun PlayerBottomBar(onOpenChannelList: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GradientScrimBottom)
            .padding(12.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        androidx.compose.material3.FilledTonalButton(onClick = onOpenChannelList) {
            Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.player_channel_list))
        }
    }
}

private val GradientScrim = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
)

private val GradientScrimBottom = androidx.compose.ui.graphics.Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
)

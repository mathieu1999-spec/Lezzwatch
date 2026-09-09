package com.lezzwatch.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.ui.components.ChannelCard
import com.lezzwatch.app.ui.components.ChannelSearchField
import com.lezzwatch.app.ui.components.EmptyState

/**
 * Bottom-sheet channel switcher shown over the player (requirement 7: pull up the channel list
 * without leaving the player). Search + favorites-only filter are enough to browse ~2,000
 * channels without a full second filter/sort UI — that lives on the dedicated Channels screen.
 */
@Composable
fun ChannelDrawerContent(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filtered = remember(channels, query, favoritesOnly) {
        channels
            .filter { !favoritesOnly || it.isFavorite }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.player_channel_list),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        ChannelSearchField(query = query, onQueryChange = { query = it })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !favoritesOnly,
                onClick = { favoritesOnly = false },
                label = { Text(stringResource(R.string.player_all_channels)) },
            )
            FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = true },
                label = { Text(stringResource(R.string.player_favorites)) },
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
                EmptyState(
                    title = stringResource(R.string.channels_empty),
                    body = "",
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onChannelSelected(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) },
                    )
                }
            }
        }
    }
}

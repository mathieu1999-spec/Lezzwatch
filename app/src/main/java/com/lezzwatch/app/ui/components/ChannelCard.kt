package com.lezzwatch.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.ui.theme.FavoriteRed

/**
 * Compact row-style card used across Home, Channels, and the in-player channel drawer, so
 * switching between those surfaces feels consistent. Kept small on purpose (spec: "don't make
 * the cards unnecessarily large" / "see many channels at once").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = true,
    onHide: (() -> Unit)? = null,
) {
    var showHideMenu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // A slightly longer hold than the ~500ms system default for a long-press, so hiding a channel
    // is a deliberate gesture rather than an accidental one — only this card's press-and-hold
    // triggers it, taps and normal long-presses elsewhere in the app are unaffected.
    val longHoldViewConfiguration = LocalViewConfiguration.current.let { base ->
        remember(base) {
            object : androidx.compose.ui.platform.ViewConfiguration by base {
                override val longPressTimeoutMillis: Long = 1_000L
            }
        }
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalViewConfiguration provides longHoldViewConfiguration) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onHide?.let {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showHideMenu = true
                            }
                        },
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChannelLogo(
                        logoUrl = channel.logoUrl,
                        modifier = Modifier
                            .size(48.dp)
                            .aspectRatio(1f),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(IntrinsicSize.Min),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${channel.country} · ${channel.genre}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showFavoriteButton) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (channel.isFavorite) R.string.player_unfavorite else R.string.player_favorite,
                                ),
                                tint = if (channel.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (onHide != null) {
            DropdownMenu(expanded = showHideMenu, onDismissRequest = { showHideMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.channels_hide_channel)) },
                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                    onClick = {
                        showHideMenu = false
                        onHide()
                    },
                )
            }
        }
    }
}

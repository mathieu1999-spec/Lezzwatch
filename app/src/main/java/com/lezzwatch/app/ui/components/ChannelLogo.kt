package com.lezzwatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.lezzwatch.app.R

/**
 * Channel logo with lazy loading + memory/disk caching (via Coil) and a clean placeholder for
 * missing/broken logos — channel art quality varies wildly across public IPTV playlists.
 */
@Composable
fun ChannelLogo(
    logoUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl.isNullOrBlank()) {
            LogoPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cd_channel_logo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = { LogoPlaceholder() },
                error = { LogoPlaceholder() },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

@Composable
private fun LogoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.LiveTv,
            contentDescription = stringResource(R.string.cd_placeholder_logo),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

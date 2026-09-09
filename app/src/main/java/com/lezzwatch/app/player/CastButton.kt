package com.lezzwatch.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Wraps Google's [MediaRouteButton] (the standard Cast icon + device-picker dialog) for use in
 * Compose. Only rendered when a [com.google.android.gms.cast.framework.CastContext] could be
 * obtained — see [PlayerViewModel.castContextOrNull] — so devices without a working Play
 * Services Cast stack simply don't show a broken button instead of crashing.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MediaRouteButton(context).also { button ->
                CastButtonFactory.setUpMediaRouteButton(context, button)
            }
        },
    )
}

package com.lezzwatch.app.data.repository

import android.content.Context
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Abstracts *where* the playlist comes from. Today there's a single implementation that reads
 * the bundled asset, but this seam is what lets a future version add a remote-URL playlist
 * (download + cache + fall back to the bundled copy) without touching [ChannelRepository],
 * the parser, or any UI code — see requirement 21 (Future Updates) in the product spec.
 */
interface PlaylistSource {
    suspend fun loadChannels(): List<Channel>
}

/** Reads `assets/playlist.m3u`, bundled with the app. */
class AssetPlaylistSource(
    private val context: Context,
    private val assetFileName: String = "playlist.m3u",
) : PlaylistSource {

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        context.assets.open(assetFileName).use { stream ->
            M3UParser.parse(stream)
        }
    }
}

/**
 * Sketch of how a future remote playlist source would slot in, left unused for now per the
 * "don't implement future features, just leave room for them" instruction:
 *
 * ```
 * class RemotePlaylistSource(
 *     private val url: String,
 *     private val httpClient: SomeHttpClient,
 *     private val fallback: PlaylistSource,
 * ) : PlaylistSource {
 *     override suspend fun loadChannels(): List<Channel> = try {
 *         M3UParser.parse(httpClient.openStream(url))
 *     } catch (e: IOException) {
 *         fallback.loadChannels()
 *     }
 * }
 * ```
 */

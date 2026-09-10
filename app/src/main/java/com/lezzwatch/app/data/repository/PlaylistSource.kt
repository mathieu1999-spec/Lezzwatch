package com.lezzwatch.app.data.repository

import android.content.Context
import android.net.Uri
import com.lezzwatch.app.data.local.prefs.PlaylistMode
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Abstracts *where* the playlist comes from, so [ChannelRepository]/[EpgRepository] and every UI
 * screen only ever depend on this interface, never on assets/files/preferences directly.
 */
interface PlaylistSource {
    suspend fun loadChannels(): List<Channel>

    /** The playlist's declared EPG (XMLTV) URL, if any — see [M3UParser.extractEpgUrl]. */
    suspend fun loadEpgUrl(): String?
}

/**
 * Reads from whichever source the user has configured in Advanced Settings (see
 * [PlaylistFileStore], [UserPreferencesRepository.setPlaylistMode]): the `assets/playlist.m3u`
 * bundled with the app by default, a playlist the user imported themselves, or nothing at all if
 * they removed the bundled one without replacing it.
 */
class ConfigurablePlaylistSource(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val playlistFileStore: PlaylistFileStore,
    private val assetFileName: String = "playlist.m3u",
) : PlaylistSource {

    override suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        openStream()?.use { M3UParser.parse(it) } ?: emptyList()
    }

    override suspend fun loadEpgUrl(): String? = withContext(Dispatchers.IO) {
        openStream()?.use { M3UParser.extractEpgUrl(it) }
    }

    private suspend fun openStream(): InputStream? =
        when (preferencesRepository.preferences.first().playlistMode) {
            PlaylistMode.REMOVED -> null
            PlaylistMode.CUSTOM -> playlistFileStore.file.takeIf { it.exists() }?.inputStream()
            PlaylistMode.BUNDLED -> context.assets.open(assetFileName)
        }
}

/**
 * Holds the on-device copy of a playlist the user imported via Advanced Settings, so it survives
 * process death without needing a persisted SAF permission on the original document URI.
 */
class PlaylistFileStore(private val context: Context) {

    val file: File get() = File(context.filesDir, "custom_playlist.m3u")

    /** Copies the picked document into app-private storage. Throws on I/O failure so the caller
     * can surface an error instead of silently switching to a playlist that isn't actually there. */
    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Unable to open file")
        input.use { source -> file.outputStream().use { output -> source.copyTo(output) } }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        file.delete()
    }
}

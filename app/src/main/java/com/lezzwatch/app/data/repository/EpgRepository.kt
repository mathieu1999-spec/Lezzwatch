package com.lezzwatch.app.data.repository

import android.util.Log
import com.lezzwatch.app.data.model.EpgProgramme
import com.lezzwatch.app.data.parser.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Fetches and parses the playlist's declared XMLTV programme guide (see
 * [PlaylistSource.loadEpgUrl]) exactly once per process lifetime, so [PlayerViewModel] can look up
 * "what's on now / next" for the channel currently being watched. A best-effort feature: playlists
 * that don't declare a guide, an unreachable guide server, or a channel simply missing from the
 * guide are all silently treated as "no EPG data" rather than an error the user needs to see.
 */
class EpgRepository(private val playlistSource: PlaylistSource) {

    private val loadMutex = Mutex()
    private var loaded = false
    private var programmesByChannel: Map<String, List<EpgProgramme>> = emptyMap()

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            programmesByChannel = try {
                val url = playlistSource.loadEpgUrl()
                if (url != null) withContext(Dispatchers.IO) { fetchAndParse(url) } else emptyMap()
            } catch (e: Exception) {
                Log.w(TAG, "EPG unavailable: ${e.message}")
                emptyMap()
            }
            loaded = true
        }
    }

    /** The programme airing right now and the one immediately after it, for [channelId]. Either
     * (or both) may be null when there's no guide data for that channel. */
    fun currentAndNext(
        channelId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Pair<EpgProgramme?, EpgProgramme?> {
        val programmes = programmesByChannel[channelId] ?: return null to null
        val current = programmes.firstOrNull { nowMillis in it.startMillis until it.stopMillis }
        val next = programmes.firstOrNull { it.startMillis >= (current?.stopMillis ?: nowMillis) }
            .takeIf { it !== current }
        return current to next
    }

    private fun fetchAndParse(url: String): Map<String, List<EpgProgramme>> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        return try {
            connection.inputStream.use { raw -> XmltvParser.parse(decompressIfGzipped(raw)) }
        } finally {
            connection.disconnect()
        }
    }

    /** Detects gzip by magic number rather than trusting the URL to end in `.gz` — more robust
     * against query strings or a server that gzips without a matching extension. */
    private fun decompressIfGzipped(raw: InputStream): InputStream {
        val buffered = BufferedInputStream(raw, 8 * 1024)
        buffered.mark(2)
        val isGzip = buffered.read() == 0x1f && buffered.read() == 0x8b
        buffered.reset()
        return if (isGzip) GZIPInputStream(buffered) else buffered
    }

    private companion object {
        const val TAG = "EpgRepository"
    }
}

package com.lezzwatch.app.player.recording

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Records the raw bytes of the currently playing HLS stream's media segments to a local file, so
 * the result is a plain, shareable video file rather than ExoPlayer's internal offline-cache
 * format. Wired in as a [DataSource.Factory] decorator around the player's HTTP data source (see
 * [com.lezzwatch.app.player.PlayerViewModel]) — every byte ExoPlayer reads from a media segment
 * is mirrored to the open output file while [isRecording] is true.
 *
 * Playlist/manifest requests (`.m3u8`) and key requests are deliberately skipped — mixing their
 * text/binary into the segment stream would corrupt the file. Concatenated MPEG-TS segments (the
 * container almost every free IPTV playlist uses) form a valid, playable `.ts` file on their own
 * with no re-muxing step needed.
 */
class StreamRecorder(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds

    private val writeLock = Any()
    @Volatile private var outputStream: OutputStream? = null
    private var pendingMediaStoreUri: Uri? = null
    private var legacyFile: File? = null
    private var timerJob: Job? = null

    /** Wraps an upstream [DataSource.Factory] so every media-segment read is mirrored into this
     * recorder while it's active. Safe to keep wrapped permanently — it's a no-op while
     * [isRecording] is false. */
    fun wrap(upstream: DataSource.Factory): DataSource.Factory =
        DataSource.Factory { RecordingDataSource(upstream.createDataSource(), this) }

    fun start(channelName: String) {
        if (_isRecording.value) return

        val safeName = channelName
            .replace(Regex("[^A-Za-z0-9-_ ]"), "")
            .trim()
            .ifBlank { "channel" }
        val fileName = "Lezzwatch_${safeName}_${TIMESTAMP_FORMAT.format(System.currentTimeMillis())}.ts"

        val stream = try {
            openOutputStream(fileName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start recording: ${e.message}")
            null
        } ?: return

        outputStream = stream
        _elapsedSeconds.value = 0
        _isRecording.value = true
        timerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun stop() {
        if (!_isRecording.value) return
        timerJob?.cancel()
        timerJob = null
        _isRecording.value = false
        _elapsedSeconds.value = 0
        synchronized(writeLock) {
            try {
                outputStream?.flush()
                outputStream?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing recording: ${e.message}")
            }
            outputStream = null
        }
        finalizeOutput()
    }

    fun release() {
        stop()
        scope.cancel()
    }

    /** Called by [RecordingDataSource] for every chunk of media-segment bytes read from the
     * network while a recording is active. No-ops when not recording. */
    fun writeChunk(buffer: ByteArray, offset: Int, length: Int) {
        if (!_isRecording.value) return
        synchronized(writeLock) {
            try {
                outputStream?.write(buffer, offset, length)
            } catch (e: Exception) {
                Log.w(TAG, "Recording write failed, stopping: ${e.message}")
                scope.launch { stop() }
            }
        }
    }

    /** Android 10+ (API 29+) writes straight into the public Movies collection via MediaStore —
     * no storage permission needed for files the app itself inserts. Older versions fall back to
     * a plain file under the public Movies directory (requires WRITE_EXTERNAL_STORAGE, requested
     * by the UI layer before calling [start]) plus an explicit media-scanner pass so the finished
     * file shows up in the gallery/file manager. */
    private fun openOutputStream(fileName: String): OutputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp2ts")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Lezzwatch")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            pendingMediaStoreUri = uri
            resolver.openOutputStream(uri) ?: error("Unable to open output stream for $uri")
        } else {
            val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Lezzwatch")
            if (!moviesDir.exists()) moviesDir.mkdirs()
            val file = File(moviesDir, fileName)
            legacyFile = file
            file.outputStream()
        }
    }

    private fun finalizeOutput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = pendingMediaStoreUri ?: return
            val values = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            runCatching { appContext.contentResolver.update(uri, values, null, null) }
            pendingMediaStoreUri = null
        } else {
            val file = legacyFile ?: return
            MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), arrayOf("video/mp2ts"), null)
            legacyFile = null
        }
    }

    private companion object {
        const val TAG = "StreamRecorder"
        val TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}

private class RecordingDataSource(
    private val upstream: DataSource,
    private val recorder: StreamRecorder,
) : DataSource by upstream {

    private var shouldRecord = false

    override fun open(dataSpec: DataSpec): Long {
        val path = dataSpec.uri.lastPathSegment.orEmpty()
        shouldRecord = !path.contains(".m3u8", ignoreCase = true) && !path.contains(".key", ignoreCase = true)
        return upstream.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytesRead = upstream.read(buffer, offset, length)
        if (shouldRecord && bytesRead > 0) {
            recorder.writeChunk(buffer, offset, bytesRead)
        }
        return bytesRead
    }
}

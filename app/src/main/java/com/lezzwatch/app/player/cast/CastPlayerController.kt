package com.lezzwatch.app.player.cast

import android.content.Context
import android.util.Log
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper around Media3's [CastPlayer] + Google's [CastContext].
 *
 * Not every device/build has Google Play Services with the Cast framework available (some
 * Android forks, some emulators, some regions) — [castContext] is null in that case and the app
 * simply doesn't show a working Cast button rather than crashing. This is the "handle the
 * failure gracefully" requirement from the product spec applied to Cast *availability* itself,
 * before a single stream is even involved.
 */
class CastPlayerController(context: Context) {

    private val appContext = context.applicationContext

    val castContext: CastContext? = try {
        CastContext.getSharedInstance(appContext)
    } catch (e: Exception) {
        Log.w(TAG, "Google Cast unavailable on this device: ${e.message}")
        null
    }

    val castPlayer: CastPlayer? = castContext?.let { ctx ->
        try {
            CastPlayer(ctx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create CastPlayer: ${e.message}")
            null
        }
    }

    private val _isCasting = MutableStateFlow(castPlayer?.isCastSessionAvailable == true)
    val isCasting: StateFlow<Boolean> = _isCasting

    val isCastAvailable: Boolean get() = castPlayer != null

    fun setSessionAvailabilityListener(
        onSessionAvailable: () -> Unit,
        onSessionUnavailable: () -> Unit,
    ) {
        castPlayer?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                _isCasting.value = true
                onSessionAvailable()
            }

            override fun onCastSessionUnavailable() {
                _isCasting.value = false
                onSessionUnavailable()
            }
        })
    }

    /** Current Cast connection state (no devices found / connecting / connected), useful for
     * showing a clearer message than a bare disabled button. */
    fun currentCastState(): Int = castContext?.castState ?: CastState.NO_DEVICES_AVAILABLE

    fun release() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
    }

    private companion object {
        const val TAG = "CastPlayerController"
    }
}

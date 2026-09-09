package com.lezzwatch.app.player.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.lezzwatch.app.util.Constants

/**
 * Registered in AndroidManifest.xml via the
 * `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME` meta-data entry, as
 * required by the Cast SDK. [Constants.CAST_RECEIVER_APP_ID] is the one thing you'd change here
 * for a production receiver app.
 */
class LezzwatchCastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(Constants.CAST_RECEIVER_APP_ID)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}

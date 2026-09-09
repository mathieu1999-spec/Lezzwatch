package com.lezzwatch.app.util

/**
 * App-wide constants that a developer configuring their own build of Lezzwatch is expected to
 * change. Kept in one file, clearly marked, on purpose — see README.md "Configuration" section.
 */
object Constants {

    /**
     * Where the "Buy Me a Coffee" button sends people. Swap this for your own page
     * (buymeacoffee.com, Ko-fi, Patreon, PayPal.me, etc.) — no payment credentials live in the
     * app itself, it's just a link to an external page.
     */
    const val SUPPORT_URL = "https://www.buymeacoffee.com/lezzwatch"

    /**
     * Google Cast receiver application ID. This defaults to Google's public "Default Media
     * Receiver", which works out of the box for casting standard HLS streams during development
     * and light use. For a published app, register your own receiver app at
     * https://cast.google.com/publish and put its ID here instead.
     */
    const val CAST_RECEIVER_APP_ID = "CC1AD845" // Default Media Receiver

    const val PRIVACY_NOTE_ABOUT_STREAMS =
        "Lezzwatch does not host or provide any streams itself; the bundled playlist is " +
            "developer-supplied configuration, not app functionality."
}

package com.lezzwatch.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the ContextWrapper chain to find the enclosing Activity, since Compose's LocalContext
 * can sometimes hand back a wrapped Context rather than the raw Activity. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return context as? Activity
}

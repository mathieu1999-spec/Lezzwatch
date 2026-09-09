# Add project specific ProGuard rules here.

# Keep Media3 / ExoPlayer classes referenced via reflection.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Google Cast framework classes.
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.cast.**

# Room
-keep class androidx.room.** { *; }

# Keep data model classes (parsed reflectively-adjacent in some flows, and used across process boundaries via Intents).
-keep class com.lezzwatch.app.data.model.** { *; }

# Kotlin coroutines / serialization safety nets
-dontwarn kotlinx.coroutines.**

package com.lezzwatch.app.player

import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lezzwatch.app.R
import com.lezzwatch.app.util.findActivity
import kotlinx.coroutines.delay

/**
 * Left half of the screen = brightness, right half = volume, both driven by vertical swipes —
 * requirement 8 in the spec. A drag must move a minimum number of pixels before anything
 * changes (see [DRAG_THRESHOLD_PX]) so incidental touches while reaching for on-screen controls
 * don't cause a jarring brightness/volume jump.
 */
@Composable
fun BrightnessVolumeGestureLayer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }

    var volumeLevel by remember {
        mutableFloatStateOf(
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) / maxVolume.toFloat(),
        )
    }
    var brightnessLevel by remember {
        mutableFloatStateOf(currentScreenBrightness(activity))
    }

    var showBrightness by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }

    LaunchedEffect(showBrightness) {
        if (showBrightness) {
            delay(1200)
            showBrightness = false
        }
    }
    LaunchedEffect(showVolume) {
        if (showVolume) {
            delay(1200)
            showVolume = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalGestureDetector(
                onLeftDrag = { deltaFraction ->
                    brightnessLevel = (brightnessLevel + deltaFraction).coerceIn(0.01f, 1f)
                    setScreenBrightness(activity, brightnessLevel)
                    showBrightness = true
                },
                onRightDrag = { deltaFraction ->
                    val newVolume = (volumeLevel + deltaFraction).coerceIn(0f, 1f)
                    volumeLevel = newVolume
                    audioManager?.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        (newVolume * maxVolume).toInt(),
                        0,
                    )
                    showVolume = true
                },
            ),
    ) {
        content()

        AnimatedVisibility(
            visible = showBrightness,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            GestureIndicator(
                icon = Icons.Filled.BrightnessMedium,
                label = stringResource(R.string.brightness_label),
                level = brightnessLevel,
            )
        }

        AnimatedVisibility(
            visible = showVolume,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            GestureIndicator(
                icon = when {
                    volumeLevel <= 0f -> Icons.Filled.VolumeOff
                    volumeLevel < 0.5f -> Icons.Filled.VolumeDown
                    else -> Icons.Filled.VolumeUp
                },
                label = stringResource(R.string.volume_label),
                level = volumeLevel,
            )
        }
    }
}

@Composable
private fun GestureIndicator(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, level: Float) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
            Text(
                text = "${(level * 100).toInt()}%",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        LinearProgressIndicator(
            progress = level,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
        )
    }
}

private const val DRAG_THRESHOLD_PX = 4f

private fun Modifier.verticalGestureDetector(
    onLeftDrag: (Float) -> Unit,
    onRightDrag: (Float) -> Unit,
): Modifier = this.pointerInput(Unit) {
    var isLeftSide = true
    var trackedHeight = size.height.toFloat().coerceAtLeast(1f)
    detectVerticalDragGestures(
        onDragStart = { offset ->
            isLeftSide = offset.x < size.width / 2f
            trackedHeight = size.height.toFloat().coerceAtLeast(1f)
        },
        onVerticalDrag = { change, dragAmount ->
            if (kotlin.math.abs(dragAmount) < DRAG_THRESHOLD_PX) return@detectVerticalDragGestures
            change.consume()
            // Negative dragAmount = finger moving up = increase. Normalize against screen height
            // so the gesture feels consistent across device sizes.
            val deltaFraction = -dragAmount / trackedHeight
            if (isLeftSide) onLeftDrag(deltaFraction) else onRightDrag(deltaFraction)
        },
    )
}

private fun currentScreenBrightness(activity: android.app.Activity?): Float {
    val current = activity?.window?.attributes?.screenBrightness ?: -1f
    return if (current in 0f..1f) current else 0.5f
}

private fun setScreenBrightness(activity: android.app.Activity?, value: Float) {
    val window = activity?.window ?: return
    val attrs: WindowManager.LayoutParams = window.attributes
    attrs.screenBrightness = value.coerceIn(0.01f, 1f)
    window.attributes = attrs
}

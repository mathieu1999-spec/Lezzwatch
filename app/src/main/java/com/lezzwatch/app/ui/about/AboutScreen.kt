package com.lezzwatch.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lezzwatch.app.BuildConfig
import com.lezzwatch.app.R
import com.lezzwatch.app.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Easter egg: 10 taps on the app name within a fast cadence reveals a hidden picture. A tap
    // that arrives slower than the window resets the count instead of just failing to increment,
    // so a hesitant tap sequence doesn't accidentally carry over into a later, unrelated one.
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAtMillis by remember { mutableLongStateOf(0L) }
    var showEasterEgg by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapAtMillis <= EASTER_EGG_TAP_WINDOW_MILLIS) tapCount + 1 else 1
                    lastTapAtMillis = now
                    if (tapCount >= EASTER_EGG_TAP_COUNT) {
                        tapCount = 0
                        showEasterEgg = true
                    }
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.about_legal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SUPPORT_URL)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.coffee_button))
            }
        }
    }

    if (showEasterEgg) {
        Dialog(
            onDismissRequest = { showEasterEgg = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showEasterEgg = false },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.easter_egg_wedding),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

private const val EASTER_EGG_TAP_COUNT = 10
private const val EASTER_EGG_TAP_WINDOW_MILLIS = 500L

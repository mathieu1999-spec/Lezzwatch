package com.lezzwatch.app.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    isInPipMode: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activePlayer by viewModel.activePlayer.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val epgGlance by viewModel.epgGlance.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var showEpgGlance by remember { mutableStateOf(false) }

    // Reset immediately on every channel switch so a stale glance never lingers under the new
    // channel's name while its own guide lookup is still in flight.
    LaunchedEffect(state.channel?.id) { showEpgGlance = false }

    // Fires again once epgGlance actually matches the channel now playing (guide lookups happen
    // asynchronously after the switch, so this can arrive slightly after the reset above).
    LaunchedEffect(epgGlance) {
        if (epgGlance != null && epgGlance?.channelId == state.channel?.id) {
            showEpgGlance = true
            delay(5000)
            showEpgGlance = false
        }
    }

    // Android 9 and below need the WRITE_EXTERNAL_STORAGE runtime permission to save a file into
    // the public Movies directory; 10+ writes through MediaStore instead, which needs no
    // permission for files the app itself creates.
    val context = LocalContext.current
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.startRecording() }

    val onToggleRecording: () -> Unit = toggle@{
        if (state.isRecording) {
            viewModel.stopRecording()
            return@toggle
        }
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            recordPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.startRecording()
        }
    }

    // Track actual play/pause so the center button reflects the real player state.
    ObservePlayingState(activePlayer) { playing -> isPlaying = playing }

    LaunchedEffect(controlsVisible, state.playbackState) {
        if (controlsVisible && state.playbackState == PlaybackUiState.Ready) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { playerView -> playerView.player = activePlayer },
        )

        if (!isInPipMode) {
            // Tapping always toggles controlsVisible, locked or not — while locked this reveals
            // just the lock button (see below) rather than the full control set, so there's
            // always a way to tap your way back to the unlock button without it sitting on
            // screen permanently.
            if (controlsLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .tapToToggle { controlsVisible = !controlsVisible },
                )
            } else {
                // Full-screen gesture layer: tap toggles the controls, vertical swipes on the
                // left half adjust brightness and on the right half adjust volume (it also draws
                // its own transient brightness/volume indicator pills).
                BrightnessVolumeGestureLayer(
                    modifier = Modifier
                        .fillMaxSize()
                        .tapToToggle { controlsVisible = !controlsVisible },
                ) {}
            }

            if (controlsVisible) {
                if (!controlsLocked) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PlayerTopBar(
                            channel = state.channel,
                            onBack = onBack,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onEnterPip = onEnterPip,
                            castButton = {
                                if (state.isCastAvailable) {
                                    CastButton(modifier = Modifier.size(48.dp))
                                }
                            },
                            isRecording = state.isRecording,
                            recordingElapsedSeconds = state.recordingElapsedSeconds,
                            canRecord = !state.isCasting,
                            onToggleRecording = onToggleRecording,
                        )
                        Spacer(Modifier.weight(1f))
                        PlayerBottomBar(
                            onOpenChannelList = { showChannelDrawer = true },
                            onChannelUp = viewModel::nextFavoriteChannel,
                            onChannelDown = viewModel::previousFavoriteChannel,
                            channelUpDownEnabled = allChannels.count { it.isFavorite } > 1,
                        )
                    }

                    if (state.playbackState == PlaybackUiState.Ready) {
                        PlayPauseButton(
                            isPlaying = isPlaying,
                            onToggle = { if (isPlaying) activePlayer.pause() else activePlayer.play() },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                // Part of the same show/hide group as the rest of the controls above — it fades
                // out with them on the same auto-hide timer instead of staying pinned on screen.
                LockButton(
                    locked = controlsLocked,
                    onToggle = { controlsLocked = !controlsLocked },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                )
            }

            AnimatedVisibility(
                visible = showEpgGlance,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                epgGlance?.let { glance ->
                    EpgGlanceOverlay(current = glance.current, next = glance.next)
                }
            }
        }

        when (state.playbackState) {
            PlaybackUiState.Loading -> LoadingOverlay(channelName = state.channel?.name)
            PlaybackUiState.NoNetwork -> ErrorOverlay(
                title = stringResource(R.string.player_no_network),
                body = "",
                onRetry = viewModel::retry,
            )
            is PlaybackUiState.Error -> ErrorOverlay(
                title = stringResource(R.string.player_error_title),
                body = stringResource(R.string.player_error_body),
                onRetry = viewModel::retry,
            )
            PlaybackUiState.Ready -> Unit
        }
    }

    if (showChannelDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showChannelDrawer = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ChannelDrawerContent(
                channels = allChannels,
                currentChannelId = state.channel?.id,
                onChannelSelected = { channel: Channel ->
                    viewModel.selectChannel(channel.id)
                    scope.launch {
                        sheetState.hide()
                        showChannelDrawer = false
                    }
                },
                onToggleFavorite = { channel: Channel -> viewModel.toggleFavoriteFor(channel) },
            )
        }
    }
}

@Composable
private fun LoadingOverlay(channelName: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            if (!channelName.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.player_loading, channelName),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(title: String, body: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (body.isNotBlank()) {
                Text(
                    body,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
                Text(stringResource(R.string.player_retry))
            }
        }
    }
}

@Composable
private fun ObservePlayingState(player: Player, onIsPlayingChanged: (Boolean) -> Unit) {
    LaunchedEffect(player) { onIsPlayingChanged(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = onIsPlayingChanged(isPlaying)
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}

private fun Modifier.tapToToggle(onTap: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures(onTap = { onTap() })
    }

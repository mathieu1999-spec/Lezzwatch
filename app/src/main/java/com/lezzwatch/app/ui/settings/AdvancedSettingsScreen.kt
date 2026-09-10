package com.lezzwatch.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lezzwatch.app.R
import com.lezzwatch.app.data.local.prefs.PlaylistMode
import kotlinx.coroutines.launch

/**
 * Lets the user replace or remove the bundled playlist. Reached only through the warning dialog
 * in [com.lezzwatch.app.ui.about.AboutScreen] — this screen itself assumes the user has already
 * been warned that changes here can leave the app with no working channels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdvancedSettingsViewModel = viewModel(factory = AdvancedSettingsViewModel.Factory),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importErrorMessage = stringResource(R.string.advanced_settings_import_error)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importPlaylist(uri) {
                scope.launch { snackbarHostState.showSnackbar(importErrorMessage) }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advanced_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
            Text(
                text = stringResource(R.string.advanced_settings_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )

            AdvancedSettingsRow(
                title = stringResource(R.string.advanced_settings_current_playlist),
                value = playlistModeLabel(prefs.playlistMode),
            )
            Divider(color = MaterialTheme.colorScheme.outline)

            AdvancedSettingsAction(
                title = stringResource(R.string.advanced_settings_import),
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
            Divider(color = MaterialTheme.colorScheme.outline)

            if (prefs.playlistMode != PlaylistMode.REMOVED) {
                AdvancedSettingsAction(
                    title = stringResource(R.string.advanced_settings_remove),
                    onClick = viewModel::removeBundledPlaylist,
                    titleColor = MaterialTheme.colorScheme.error,
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }

            if (prefs.playlistMode != PlaylistMode.BUNDLED) {
                AdvancedSettingsAction(
                    title = stringResource(R.string.advanced_settings_restore),
                    onClick = viewModel::restoreBundledPlaylist,
                )
                Divider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AdvancedSettingsRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdvancedSettingsAction(
    title: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Unspecified,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
    }
}

@Composable
private fun playlistModeLabel(mode: PlaylistMode): String = when (mode) {
    PlaylistMode.BUNDLED -> stringResource(R.string.advanced_settings_mode_bundled)
    PlaylistMode.CUSTOM -> stringResource(R.string.advanced_settings_mode_custom)
    PlaylistMode.REMOVED -> stringResource(R.string.advanced_settings_mode_removed)
}

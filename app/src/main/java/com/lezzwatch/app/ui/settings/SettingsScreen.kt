package com.lezzwatch.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lezzwatch.app.R
import com.lezzwatch.app.data.local.prefs.AppTheme
import com.lezzwatch.app.data.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showClearFavoritesDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {

            // Theme
            Box {
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    value = themeLabel(prefs.theme),
                    onClick = { showThemeMenu = true },
                )
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(themeLabel(theme)) },
                            leadingIcon = {
                                RadioButton(selected = prefs.theme == theme, onClick = null)
                            },
                            onClick = { showThemeMenu = false; viewModel.setTheme(theme) },
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            // Autoplay last channel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_autoplay), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_autoplay_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = prefs.autoPlayLastChannel,
                    onCheckedChange = viewModel::setAutoPlayLastChannel,
                )
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            // Default sort order
            Box {
                SettingsRow(
                    title = stringResource(R.string.settings_default_sort),
                    value = sortLabel(prefs.defaultSortOption),
                    onClick = { showSortMenu = true },
                )
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(option)) },
                            onClick = { showSortMenu = false; viewModel.setDefaultSortOption(option) },
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline)

            // Clear favorites
            SettingsRow(
                title = stringResource(R.string.settings_clear_favorites),
                value = null,
                onClick = { showClearFavoritesDialog = true },
                titleColor = MaterialTheme.colorScheme.error,
            )
            Divider(color = MaterialTheme.colorScheme.outline)

            // About
            SettingsRow(
                title = stringResource(R.string.settings_about),
                value = null,
                onClick = onOpenAbout,
            )
        }
    }

    if (showClearFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showClearFavoritesDialog = false },
            title = { Text(stringResource(R.string.settings_clear_favorites_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_favorites_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearFavorites()
                    showClearFavoritesDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearFavoritesDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
    AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
    AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
}

@Composable
private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.NAME_ASC -> stringResource(R.string.sort_az)
    SortOption.NAME_DESC -> stringResource(R.string.sort_za)
    SortOption.COUNTRY -> stringResource(R.string.sort_country)
    SortOption.GENRE -> stringResource(R.string.sort_genre)
    SortOption.FAVORITES_FIRST -> stringResource(R.string.sort_favorites_first)
}

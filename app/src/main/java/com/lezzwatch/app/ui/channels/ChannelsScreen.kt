package com.lezzwatch.app.ui.channels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.ui.components.ChannelCard
import com.lezzwatch.app.ui.components.ChannelSearchField
import com.lezzwatch.app.ui.components.EmptyState
import com.lezzwatch.app.ui.components.FilterDropdownChip
import com.lezzwatch.app.ui.components.SortMenuButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = viewModel(factory = ChannelsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allGenresLabel = stringResource(R.string.channels_all_genres)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channels_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChannelSearchField(
                    query = state.filter.query,
                    onQueryChange = viewModel::onQueryChange,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterDropdownChip(
                        label = state.filter.genre ?: allGenresLabel,
                        selected = state.filter.genre != null,
                        options = state.availableGenres,
                        allLabel = allGenresLabel,
                        onOptionSelected = viewModel::onGenreSelected,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.channels_results_count, state.channels.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SortMenuButton(
                        currentSort = state.filter.sortOption,
                        onSortSelected = viewModel::onSortSelected,
                    )
                }
            }

            when {
                !state.isLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.channels.isEmpty() -> EmptyState(
                    title = stringResource(R.string.channels_empty),
                    body = "",
                    actionLabel = stringResource(R.string.channels_clear_filters),
                    onAction = viewModel::clearFilters,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.channels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onChannelSelected(channel) },
                            onToggleFavorite = { viewModel.toggleFavorite(channel) },
                        )
                    }
                }
            }
        }
    }
}

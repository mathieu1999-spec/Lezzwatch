package com.lezzwatch.app.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.model.ChannelFilter
import com.lezzwatch.app.data.model.SortOption
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelsUiState(
    val isLoaded: Boolean = false,
    val filter: ChannelFilter = ChannelFilter(),
    val channels: List<Channel> = emptyList(),
    val availableGenres: List<String> = emptyList(),
)

class ChannelsViewModel(
    private val channelRepository: ChannelRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(ChannelFilter())

    val uiState: StateFlow<ChannelsUiState> = combine(
        channelRepository.channels,
        channelRepository.isLoaded,
        filterState,
    ) { channels, isLoaded, filter ->
        ChannelsUiState(
            isLoaded = isLoaded,
            filter = filter,
            channels = applyFilter(channels, filter),
            availableGenres = channels.map { it.genre }.distinct().sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChannelsUiState())

    init {
        viewModelScope.launch {
            channelRepository.ensureLoaded()
            val defaultSort = preferencesRepository.preferences.first().defaultSortOption
            filterState.value = filterState.value.copy(sortOption = defaultSort)
        }
    }

    fun onQueryChange(query: String) {
        filterState.value = filterState.value.copy(query = query)
    }

    fun onGenreSelected(genre: String?) {
        filterState.value = filterState.value.copy(genre = genre)
    }

    fun onSortSelected(sortOption: SortOption) {
        filterState.value = filterState.value.copy(sortOption = sortOption)
    }

    fun clearFilters() {
        filterState.value = ChannelFilter(sortOption = filterState.value.sortOption)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { channelRepository.toggleFavorite(channel) }
    }

    private fun applyFilter(channels: List<Channel>, filter: ChannelFilter): List<Channel> {
        var result = channels

        if (filter.query.isNotBlank()) {
            val q = filter.query.trim()
            result = result.filter { it.name.contains(q, ignoreCase = true) }
        }
        if (filter.genre != null) {
            result = result.filter { it.genre == filter.genre }
        }

        result = when (filter.sortOption) {
            SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            SortOption.COUNTRY -> result.sortedWith(compareBy({ it.country }, { it.name.lowercase() }))
            SortOption.GENRE -> result.sortedWith(compareBy({ it.genre }, { it.name.lowercase() }))
            SortOption.FAVORITES_FIRST -> result.sortedWith(
                compareByDescending<Channel> { it.isFavorite }.thenBy { it.name.lowercase() },
            )
        }

        return result
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ChannelsViewModel(container.channelRepository, container.userPreferencesRepository)
            }
        }
    }
}

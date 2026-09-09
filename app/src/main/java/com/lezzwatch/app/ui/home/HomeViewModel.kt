package com.lezzwatch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val channelRepository: ChannelRepository) : ViewModel() {

    val favoriteChannels: StateFlow<List<Channel>> = channelRepository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoaded: StateFlow<Boolean> = channelRepository.isLoaded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { channelRepository.ensureLoaded() }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { channelRepository.toggleFavorite(channel) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(appContainer().channelRepository)
            }
        }
    }
}

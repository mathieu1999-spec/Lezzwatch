package com.lezzwatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lezzwatch.app.data.model.Channel
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HiddenChannelsViewModel(private val channelRepository: ChannelRepository) : ViewModel() {

    val hiddenChannels: StateFlow<List<Channel>> = channelRepository.channels
        .map { list -> list.filter { it.isHidden }.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unhideChannel(channel: Channel) {
        viewModelScope.launch { channelRepository.unhideChannel(channel) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HiddenChannelsViewModel(appContainer().channelRepository)
            }
        }
    }
}

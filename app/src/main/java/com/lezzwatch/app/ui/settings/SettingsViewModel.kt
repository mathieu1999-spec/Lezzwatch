package com.lezzwatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lezzwatch.app.data.local.prefs.AppTheme
import com.lezzwatch.app.data.local.prefs.UserPreferences
import com.lezzwatch.app.data.local.prefs.UserPreferencesRepository
import com.lezzwatch.app.data.model.SortOption
import com.lezzwatch.app.data.repository.ChannelRepository
import com.lezzwatch.app.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesRepository.setTheme(theme) }
    }

    fun setAutoPlayLastChannel(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoPlayLastChannel(enabled) }
    }

    fun setDefaultSortOption(sortOption: SortOption) {
        viewModelScope.launch { preferencesRepository.setDefaultSortOption(sortOption) }
    }

    fun clearFavorites() {
        viewModelScope.launch { channelRepository.clearFavorites() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(container.userPreferencesRepository, container.channelRepository)
            }
        }
    }
}

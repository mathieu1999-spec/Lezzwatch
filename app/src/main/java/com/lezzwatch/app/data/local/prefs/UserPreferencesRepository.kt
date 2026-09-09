package com.lezzwatch.app.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lezzwatch.app.data.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lezzwatch_prefs")

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val theme: AppTheme = AppTheme.DARK,
    val autoPlayLastChannel: Boolean = false,
    val defaultSortOption: SortOption = SortOption.NAME_ASC,
    val lastWatchedChannelId: String? = null,
)

/**
 * Small wrapper around Jetpack DataStore for the handful of lightweight settings the app needs.
 * Favorites live in Room instead (see [com.lezzwatch.app.data.local.db.LezzwatchDatabase]) since
 * they're a growing list of records rather than single key/value settings.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val AUTOPLAY_LAST = booleanPreferencesKey("autoplay_last_channel")
        val DEFAULT_SORT = stringPreferencesKey("default_sort_option")
        val LAST_CHANNEL_ID = stringPreferencesKey("last_watched_channel_id")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs: Preferences ->
        UserPreferences(
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: AppTheme.DARK,
            autoPlayLastChannel = prefs[Keys.AUTOPLAY_LAST] ?: false,
            defaultSortOption = prefs[Keys.DEFAULT_SORT]
                ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
                ?: SortOption.NAME_ASC,
            lastWatchedChannelId = prefs[Keys.LAST_CHANNEL_ID],
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAutoPlayLastChannel(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_LAST] = enabled }
    }

    suspend fun setDefaultSortOption(sortOption: SortOption) {
        context.dataStore.edit { it[Keys.DEFAULT_SORT] = sortOption.name }
    }

    suspend fun setLastWatchedChannelId(channelId: String?) {
        context.dataStore.edit {
            if (channelId == null) it.remove(Keys.LAST_CHANNEL_ID) else it[Keys.LAST_CHANNEL_ID] = channelId
        }
    }
}

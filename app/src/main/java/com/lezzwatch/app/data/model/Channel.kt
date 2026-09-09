package com.lezzwatch.app.data.model

/**
 * Domain model for a single IPTV channel.
 *
 * Instances are immutable; [isFavorite] is a derived flag stitched on by
 * [com.lezzwatch.app.data.repository.ChannelRepository] when it combines the parsed playlist
 * with the favorite IDs stored in Room, so the parser itself never needs to know about
 * favorites at all.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val country: String,
    val genre: String,
    val group: String?,
    val isFavorite: Boolean = false,
) {
    companion object {
        const val UNKNOWN_COUNTRY = "International"
        const val UNKNOWN_GENRE = "General"
    }
}

/** Sort options exposed on the Channels screen. Order here is the order shown in the sort menu.
 * Display labels are resolved per-screen via `sortLabel()` composables (see ChannelsScreen,
 * SettingsScreen, FilterSortBar) so they can pull from string resources with proper locale
 * support. */
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    COUNTRY,
    GENRE,
    FAVORITES_FIRST,
}

/** Current filter + sort selection for the Channels screen, kept together so it's easy to persist.
 *
 * [selectedGenres] `null` means "no filter" (every genre shown) — the default state, and the
 * canonical form once every available genre ends up individually selected again. A non-null set
 * (including an empty one, meaning nothing is selected and every channel is hidden) is used once
 * the user deselects at least one genre from the full set. */
data class ChannelFilter(
    val query: String = "",
    val selectedGenres: Set<String>? = null,
    val sortOption: SortOption = SortOption.NAME_ASC,
)

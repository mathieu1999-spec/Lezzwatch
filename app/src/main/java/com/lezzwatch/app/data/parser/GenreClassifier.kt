package com.lezzwatch.app.data.parser

import com.lezzwatch.app.data.model.Channel

/**
 * Derives a genre for a channel when the playlist doesn't carry real category metadata.
 *
 * Many public M3U playlists put a single language/region tag in `group-title` (the bundled
 * playlist uses `"English"` for every single channel) rather than an actual genre, so it can't
 * be trusted as a genre source. Instead we classify by keyword match against the channel name,
 * which works reasonably well for real-world channel naming conventions. If a future playlist
 * *does* carry a genuine `group-title` category (something other than a language name), prefer
 * that — see [classify].
 */
object GenreClassifier {

    // Group-title values that are actually language/region tags, not genres, and should be
    // ignored as a genre source. Extend this if a replacement playlist uses similar tags.
    private val NON_GENRE_GROUP_TITLES = setOf(
        "english", "spanish", "french", "german", "portuguese", "arabic", "italian", "russian",
        "hindi", "chinese", "polish", "turkish", "dutch", "undefined", "unknown",
    )

    private val keywordRules: List<Pair<Regex, String>> = listOf(
        Regex("news|cnn|bbc\\s*news|msnbc|fox\\s*news|al\\s*jazeera|bloomberg|weather", RegexOption.IGNORE_CASE) to "News",
        Regex("sport|espn|football|soccer|nba|nfl|nhl|mlb|cricket|golf|rugby|tennis|fight|boxing|ufc|motor\\s*sport|racing", RegexOption.IGNORE_CASE) to "Sports",
        Regex("movie|cinema|film|hollywood|action|thriller|western(?!\\w)", RegexOption.IGNORE_CASE) to "Movies",
        Regex("kids?|cartoon|toon|nick(?:elodeon)?|disney|junior|baby", RegexOption.IGNORE_CASE) to "Kids",
        Regex("music|hits|radio|mtv|vh1|concert|jazz|country\\s*music|hip\\s*hop", RegexOption.IGNORE_CASE) to "Music",
        Regex("document(ary|aries)|discovery|history|nat\\s*geo|nature|science|animal\\s*planet", RegexOption.IGNORE_CASE) to "Documentary",
        Regex("shop(ping)?|qvc|hsn", RegexOption.IGNORE_CASE) to "Shopping",
        Regex("religio|church|gospel|christian|islam|catholic|faith|worship|bible", RegexOption.IGNORE_CASE) to "Religious",
        Regex("comedy|sitcom|laugh", RegexOption.IGNORE_CASE) to "Comedy",
        Regex("drama|soap|telenovela", RegexOption.IGNORE_CASE) to "Drama",
        Regex("lifestyle|home|garden|food|cooking|travel|fashion", RegexOption.IGNORE_CASE) to "Lifestyle",
        Regex("adult|xxx|18\\+", RegexOption.IGNORE_CASE) to "Adult",
        Regex("business|finance|market", RegexOption.IGNORE_CASE) to "Business",
        Regex("entertain|variety|reality|talk\\s*show", RegexOption.IGNORE_CASE) to "Entertainment",
    )

    fun classify(channelName: String, groupTitle: String?): String {
        val cleanGroup = groupTitle?.trim()
        if (!cleanGroup.isNullOrEmpty() && cleanGroup.lowercase() !in NON_GENRE_GROUP_TITLES) {
            return cleanGroup
        }
        for ((pattern, genre) in keywordRules) {
            if (pattern.containsMatchIn(channelName)) return genre
        }
        return Channel.UNKNOWN_GENRE
    }
}

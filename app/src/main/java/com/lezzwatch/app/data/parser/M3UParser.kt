package com.lezzwatch.app.data.parser

import com.lezzwatch.app.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * Parses Extended M3U playlists into [Channel] objects.
 *
 * Designed to be tolerant of the messiness typical of real-world IPTV playlists: missing
 * attributes, missing logos, duplicate/absent `tvg-id`s, stray blank lines, a UTF-8 BOM, and
 * extra `#EXT...` directives we don't understand (`#EXTVLCOPT`, `#EXTGRP`, etc. are simply
 * skipped). A malformed entry is dropped rather than aborting the whole parse, so one bad line
 * in a 2,000-channel playlist never breaks the app.
 */
object M3UParser {

    private val extinfAttrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val tvgIdCountryRegex = Regex("""\.([a-zA-Z]{2,3})@""")

    fun parse(input: InputStream): List<Channel> {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val channels = mutableListOf<Channel>()

        var pendingDuration: String? = null
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingDisplayName: String? = null
        var sawExtm3uHeader = false

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.removePrefix("﻿").trim()
                if (line.isEmpty()) continue

                when {
                    line.startsWith("#EXTM3U") -> {
                        sawExtm3uHeader = true
                    }

                    line.startsWith("#EXTINF") -> {
                        val (duration, attrs, displayName) = parseExtinf(line)
                        pendingDuration = duration
                        pendingAttrs = attrs
                        pendingDisplayName = displayName
                    }

                    line.startsWith("#") -> {
                        // Unknown/unsupported directive (#EXTVLCOPT, #EXTGRP, #KODIPROP, custom
                        // x-tags, etc.) — intentionally ignored rather than treated as an error.
                    }

                    else -> {
                        // First non-comment line after an #EXTINF is the stream URL.
                        val streamUrl = line
                        if (streamUrl.isNotBlank() && pendingDisplayName != null) {
                            channels += buildChannel(pendingAttrs, pendingDisplayName!!, streamUrl)
                        } else if (streamUrl.isNotBlank()) {
                            // A URL with no preceding #EXTINF — still usable, just unnamed.
                            channels += buildChannel(emptyMap(), fallbackName(streamUrl), streamUrl)
                        }
                        pendingDuration = null
                        pendingAttrs = emptyMap()
                        pendingDisplayName = null
                    }
                }
            }
        }

        // Not every valid-ish playlist starts with a proper #EXTM3U header; we still parse it,
        // sawExtm3uHeader is only kept around for potential future diagnostics/telemetry.
        @Suppress("UNUSED_EXPRESSION")
        sawExtm3uHeader

        return dedupe(channels)
    }

    private fun parseExtinf(line: String): Triple<String?, Map<String, String>, String> {
        // Format: #EXTINF:<duration> <key="value" ...>,<display name>
        val body = line.removePrefix("#EXTINF:")
        val commaIndex = lastTopLevelComma(body)
        val head = if (commaIndex >= 0) body.substring(0, commaIndex) else body
        val displayName = if (commaIndex >= 0) body.substring(commaIndex + 1).trim() else ""

        val duration = head.trim().substringBefore(' ').ifBlank { null }
        val attrs = extinfAttrRegex.findAll(head).associate { it.groupValues[1] to it.groupValues[2] }

        return Triple(duration, attrs, displayName.ifBlank { attrs["tvg-name"] ?: "Unnamed Channel" })
    }

    /**
     * The display name can itself contain commas, so we look for the LAST comma that isn't
     * inside a quoted attribute value — approximated here by taking the last comma in the
     * string, since attribute values are already comma-free in practice for tvg-id/tvg-logo/etc.
     * This mirrors how most real-world M3U parsers handle it.
     */
    private fun lastTopLevelComma(head: String): Int = head.lastIndexOf(',')

    private fun buildChannel(attrs: Map<String, String>, displayName: String, streamUrl: String): Channel {
        val tvgId = attrs["tvg-id"]?.trim()
        val tvgName = attrs["tvg-name"]?.trim()
        val logoUrl = attrs["tvg-logo"]?.trim()?.takeIf { it.isNotBlank() }
        val groupTitle = attrs["group-title"]?.trim()?.takeIf { it.isNotBlank() }

        val name = displayName.trim().ifBlank { tvgName?.ifBlank { null } ?: fallbackName(streamUrl) }
        val country = resolveCountry(tvgId, name)
        val genre = GenreClassifier.classify(name, groupTitle)
        val id = tvgId?.takeIf { it.isNotBlank() } ?: stableIdFor(name, streamUrl)

        return Channel(
            id = id,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            country = country,
            genre = genre,
            group = groupTitle,
        )
    }

    private fun resolveCountry(tvgId: String?, name: String): String {
        if (!tvgId.isNullOrBlank()) {
            val match = tvgIdCountryRegex.find(tvgId)
            val code = match?.groupValues?.get(1)
            CountryCodes.nameFor(code)?.let { return it }
        }
        // Fallback: look for a trailing " (XX)" / "[XX]" country hint in the display name.
        val bracketMatch = Regex("""[\[(]([A-Za-z]{2})[\])]""").find(name)
        CountryCodes.nameFor(bracketMatch?.groupValues?.get(1))?.let { return it }
        return Channel.UNKNOWN_COUNTRY
    }

    private fun fallbackName(streamUrl: String): String =
        streamUrl.substringAfterLast('/').substringBefore('?').ifBlank { "Unnamed Channel" }

    /** Deterministic ID for channels lacking a usable tvg-id, so re-parsing the same playlist
     * (e.g. after an app update) keeps favorites intact. */
    private fun stableIdFor(name: String, streamUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$name|$streamUrl".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    /** De-duplicates by (name, streamUrl) pair — the same channel can legitimately appear more
     * than once in aggregated playlists with an identical stream, which just clutters browsing. */
    private fun dedupe(channels: List<Channel>): List<Channel> {
        val seen = HashSet<String>()
        val result = ArrayList<Channel>(channels.size)
        for (channel in channels) {
            val key = "${channel.name}|${channel.streamUrl}"
            if (seen.add(key)) result += channel
        }
        return result
    }
}

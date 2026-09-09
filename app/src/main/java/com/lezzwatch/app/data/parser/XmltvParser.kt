package com.lezzwatch.app.data.parser

import android.util.Xml
import com.lezzwatch.app.data.model.EpgProgramme
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses an XMLTV guide (the format nearly every IPTV EPG source uses) into a per-channel list
 * of programmes, keyed by the same `channel` id XMLTV `<programme>` elements use — which lines up
 * with [Channel.id] whenever a playlist entry has a `tvg-id` (see [M3UParser.buildChannel]).
 *
 * Only `<programme>` elements are of interest here — the `<channel>` display-name/icon elements
 * are ignored since we already have that information (and better logos) from the M3U playlist
 * itself.
 */
object XmltvParser {

    // XMLTV timestamps look like "20260908013000 +0000".
    private val timestampFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(input: InputStream): Map<String, List<EpgProgramme>> {
        val programmesByChannel = HashMap<String, MutableList<EpgProgramme>>()
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        var channelId: String? = null
        var startMillis: Long? = null
        var stopMillis: Long? = null
        var title: String? = null
        var inTitle = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channelId = parser.getAttributeValue(null, "channel")
                        startMillis = parseTimestamp(parser.getAttributeValue(null, "start"))
                        stopMillis = parseTimestamp(parser.getAttributeValue(null, "stop"))
                        title = null
                    }
                    "title" -> inTitle = true
                }

                XmlPullParser.TEXT -> if (inTitle) {
                    title = (title.orEmpty()) + parser.text
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> inTitle = false
                    "programme" -> {
                        val id = channelId
                        val start = startMillis
                        val stop = stopMillis
                        if (!id.isNullOrBlank() && start != null && stop != null) {
                            programmesByChannel.getOrPut(id) { mutableListOf() } +=
                                EpgProgramme(title.orEmpty().trim(), start, stop)
                        }
                        channelId = null
                        startMillis = null
                        stopMillis = null
                        title = null
                    }
                }
            }
            eventType = parser.next()
        }

        return programmesByChannel.mapValues { (_, programmes) -> programmes.sortedBy { it.startMillis } }
    }

    private fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            timestampFormat.parse(raw)?.time
        } catch (e: Exception) {
            null
        }
    }
}

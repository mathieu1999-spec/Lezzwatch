package com.lezzwatch.app.data.model

/** A single programme entry from an XMLTV guide, scoped to one channel. */
data class EpgProgramme(
    val title: String,
    val startMillis: Long,
    val stopMillis: Long,
)

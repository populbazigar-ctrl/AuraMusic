package com.aura.music

import android.content.Context
import android.net.Uri
import java.io.File

object AuraLyricsEngine {

    private val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    fun findSidecarLyrics(track: AuraTrack): List<LyricLine> {
        if (track.filePath.isBlank()) return emptyList()
        return runCatching {
            val audioFile = File(track.filePath)
            val parent = audioFile.parentFile ?: return emptyList()
            val baseName = audioFile.nameWithoutExtension
            val lrcFile = File(parent, "$baseName.lrc")
            val txtFile = File(parent, "$baseName.txt")

            when {
                lrcFile.exists() && lrcFile.canRead() -> parseRawText(lrcFile.readText())
                txtFile.exists() && txtFile.canRead() -> parseRawText(txtFile.readText())
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    fun loadFromUri(context: Context, uri: Uri): List<LyricLine> {
        return runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: ""
            parseRawText(content)
        }.getOrDefault(emptyList())
    }

    fun parseRawText(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val lines = raw.lines()
        val parsed = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val match = lrcRegex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val ms = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                val totalMs = (min * 60_000L) + (sec * 1_000L) + ms
                if (text.isNotEmpty()) {
                    parsed.add(LyricLine(timeMs = totalMs, text = text))
                }
            } else if (!trimmed.startsWith("[")) {
                parsed.add(LyricLine(timeMs = -1L, text = trimmed))
            }
        }
        return parsed.sortedBy { if (it.timeMs >= 0L) it.timeMs else Long.MAX_VALUE }
    }

    fun activeLineIndex(lyrics: List<LyricLine>, positionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        val hasTimestamps = lyrics.any { it.timeMs >= 0L }
        if (!hasTimestamps) return -1
        var index = -1
        for (i in lyrics.indices) {
            val stamp = lyrics[i].timeMs
            if (stamp in 0..positionMs) {
                index = i
            }
        }
        return index
    }
}

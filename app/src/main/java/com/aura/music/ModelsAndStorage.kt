package com.aura.music

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

enum class AuraSortOrder(val label: String) {
    TITLE("Title A-Z"),
    ARTIST("Artist A-Z"),
    RECENT("Recently Added")
}

enum class LibraryTab(val title: String) {
    HOME("Home"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

data class AuraTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val folderName: String,
    val filePath: String,
    val dateAddedSec: Long
)

data class AuraPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class AuraLocalStorage(context: Context) {
    private val prefs = context.getSharedPreferences("aura_music_vault", Context.MODE_PRIVATE)

    fun isDarkTheme(): Boolean = prefs.getBoolean("dark_theme", true)

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun getSortOrder(): AuraSortOrder {
        val raw = prefs.getString("sort_order", AuraSortOrder.TITLE.name) ?: AuraSortOrder.TITLE.name
        return runCatching { AuraSortOrder.valueOf(raw) }.getOrDefault(AuraSortOrder.TITLE)
    }

    fun setSortOrder(order: AuraSortOrder) {
        prefs.edit().putString("sort_order", order.name).apply()
    }

    fun getFavorites(): Set<Long> {
        val rawSet = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun saveFavorites(ids: Set<Long>) {
        prefs.edit().putStringSet("favorite_ids", ids.map { it.toString() }.toSet()).apply()
    }

    fun getRecentIds(): List<Long> {
        val raw = prefs.getString("recent_ids", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun pushRecentId(id: Long) {
        val updated = (listOf(id) + getRecentIds().filter { it != id }).take(30)
        prefs.edit().putString("recent_ids", updated.joinToString(",")).apply()
    }

    fun getSavedSafUris(): List<String> {
        val raw = prefs.getString("saf_uris", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.getString(idx) }
        }.getOrDefault(emptyList())
    }

    fun addSafUri(uriString: String) {
        val current = getSavedSafUris().toMutableList()
        if (!current.contains(uriString)) {
            current.add(uriString)
            val arr = JSONArray()
            current.forEach { arr.put(it) }
            prefs.edit().putString("saf_uris", arr.toString()).apply()
        }
    }

    fun getPlaylists(): List<AuraPlaylist> {
        val raw = prefs.getString("playlists_json", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val tracksJson = obj.getJSONArray("tracks")
                    val tracks = List(tracksJson.length()) { t -> tracksJson.getLong(t) }
                    add(AuraPlaylist(id = id, name = name, trackIds = tracks))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePlaylists(playlists: List<AuraPlaylist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            val tracksArr = JSONArray()
            pl.trackIds.forEach { tracksArr.put(it) }
            obj.put("tracks", tracksArr)
            arr.put(obj)
        }
        prefs.edit().putString("playlists_json", arr.toString()).apply()
    }
}

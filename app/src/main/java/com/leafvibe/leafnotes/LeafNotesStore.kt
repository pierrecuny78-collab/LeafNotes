package com.leafvibe.leafnotes

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single persistence boundary for the local, offline-first library.
 * Each write uses SharedPreferences.apply(); values survive process restarts.
 */
internal class LeafNotesStore(private val preferences: SharedPreferences) {
    fun loadNotes(): List<Note> = runCatching {
        val array = JSONArray(preferences.getString(KEY_NOTES, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Note(
                id = id,
                title = item.optString("title"),
                body = item.optString("body"),
                category = item.optString("category", "Nouveaux"),
                favorite = item.optBoolean("favorite", false),
                updatedAt = item.optLong("updatedAt", 0L)
            )
        }
    }.getOrDefault(emptyList())

    fun saveNotes(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject()
                .put("id", note.id)
                .put("title", note.title)
                .put("body", note.body)
                .put("category", note.category)
                .put("favorite", note.favorite)
                .put("updatedAt", note.updatedAt))
        }
        preferences.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun loadCategories(defaults: List<NoteCategory>): List<NoteCategory> = runCatching {
        val raw = preferences.getString(KEY_CATEGORIES, null) ?: return@runCatching defaults
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            NoteCategory(
                name = name,
                emoji = item.optString("emoji", "📁"),
                color = item.optLong("color", 0xFF6C9B80),
                parent = item.optString("parent").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }.getOrDefault(defaults)

    fun saveCategories(categories: List<NoteCategory>) {
        val array = JSONArray()
        categories.forEach { category ->
            array.put(JSONObject()
                .put("name", category.name)
                .put("emoji", category.emoji)
                .put("color", category.color)
                .put("parent", category.parent ?: JSONObject.NULL))
        }
        preferences.edit().putString(KEY_CATEGORIES, array.toString()).apply()
    }

    fun loadTrash(): List<TrashedNote> = runCatching {
        val array = JSONArray(preferences.getString(KEY_TRASH, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val note = item.optJSONObject("note") ?: return@mapNotNull null
            val id = note.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TrashedNote(
                note = Note(
                    id = id,
                    title = note.optString("title"),
                    body = note.optString("body"),
                    category = note.optString("category", "Nouveaux"),
                    favorite = note.optBoolean("favorite", false),
                    updatedAt = note.optLong("updatedAt", 0L)
                ),
                deletedAt = item.optLong("deletedAt", 0L)
            )
        }
    }.getOrDefault(emptyList())

    fun saveTrash(trash: List<TrashedNote>) {
        val array = JSONArray()
        trash.forEach { entry ->
            val note = entry.note
            array.put(JSONObject()
                .put("deletedAt", entry.deletedAt)
                .put("note", JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("category", note.category)
                    .put("favorite", note.favorite)
                    .put("updatedAt", note.updatedAt)))
        }
        preferences.edit().putString(KEY_TRASH, array.toString()).apply()
    }

    companion object {
        private const val KEY_NOTES = "notes"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_TRASH = "trash"
    }
}

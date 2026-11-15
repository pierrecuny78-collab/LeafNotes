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
                updatedAt = item.optLong("updatedAt", 0L),
                tags = item.optJSONArray("tags").toStringList(),
                checklist = item.optJSONArray("checklist").toChecklist(),
                flashcards = item.optJSONArray("flashcards").toFlashcards(),
                icon = item.optString("icon", "📝"),
                reminderAt = item.optLong("reminderAt", 0L),
                revisions = item.optJSONArray("revisions").toRevisions(id)
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
                .put("updatedAt", note.updatedAt)
                .put("tags", JSONArray(note.tags))
                .put("checklist", JSONArray().apply { note.checklist.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked).put("position", it.position)) } })
                .put("flashcards", note.flashcards.toJson())
                .put("icon", note.icon)
                .put("reminderAt", note.reminderAt)
                .put("revisions", note.revisions.toJson()))
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


    fun loadTemplates(): List<NoteTemplate> = runCatching {
        val array = JSONArray(preferences.getString(KEY_TEMPLATES, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").ifBlank { return@mapNotNull null }
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            NoteTemplate(id, name, item.optString("title"), item.optString("body"), item.optString("category").takeIf { it.isNotBlank() && it != "null" })
        }
    }.getOrDefault(emptyList())

    fun saveTemplates(templates: List<NoteTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(JSONObject().put("id", template.id).put("name", template.name)
                .put("title", template.title).put("body", template.body)
                .put("category", template.category ?: JSONObject.NULL))
        }
        preferences.edit().putString(KEY_TEMPLATES, array.toString()).apply()
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
                    updatedAt = note.optLong("updatedAt", 0L),
                    tags = note.optJSONArray("tags").toStringList(),
                    checklist = note.optJSONArray("checklist").toChecklist(),
                    flashcards = note.optJSONArray("flashcards").toFlashcards(),
                    icon = note.optString("icon", "📝"),
                    reminderAt = note.optLong("reminderAt", 0L),
                    revisions = note.optJSONArray("revisions").toRevisions(id)
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
                    .put("updatedAt", note.updatedAt)
                    .put("tags", JSONArray(note.tags))
                    .put("checklist", JSONArray().apply { note.checklist.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked).put("position", it.position)) } })
                    .put("flashcards", note.flashcards.toJson())
                    .put("icon", note.icon)
                    .put("reminderAt", note.reminderAt)
                    .put("revisions", note.revisions.toJson())))
        }
        preferences.edit().putString(KEY_TRASH, array.toString()).apply()
    }

    companion object {
        private const val KEY_NOTES = "notes"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_TRASH = "trash"
        private const val KEY_TEMPLATES = "templates"
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}

private fun JSONArray?.toChecklist(): List<ChecklistItem> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        val text = item.optString("text").trim()
        if (text.isBlank()) null else ChecklistItem(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            text = text,
            checked = item.optBoolean("checked", false),
            position = item.optInt("position", index)
        )
    }
}

private fun List<NoteRevision>.toJson(): JSONArray = JSONArray().apply {
    forEach { revision -> put(JSONObject().put("id", revision.id).put("noteId", revision.noteId).put("title", revision.title).put("body", revision.body).put("savedAt", revision.savedAt)) }
}

private fun JSONArray?.toRevisions(noteId: String): List<NoteRevision> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        NoteRevision(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            noteId = noteId,
            title = item.optString("title"),
            body = item.optString("body"),
            savedAt = item.optLong("savedAt", 0L)
        )
    }
}

private fun List<Flashcard>.toJson(): JSONArray = JSONArray().apply {
    forEach { card ->
        put(JSONObject().put("id", card.id).put("question", card.question).put("answer", card.answer)
            .put("dueAt", card.dueAt).put("intervalDays", card.intervalDays).put("repetitions", card.repetitions))
    }
}

private fun JSONArray?.toFlashcards(): List<Flashcard> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        val question = item.optString("question").trim()
        val answer = item.optString("answer").trim()
        if (question.isBlank() || answer.isBlank()) null else Flashcard(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            question = question,
            answer = answer,
            dueAt = item.optLong("dueAt", 0L),
            intervalDays = item.optInt("intervalDays", 0),
            repetitions = item.optInt("repetitions", 0)
        )
    }
}

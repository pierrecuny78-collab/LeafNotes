package com.leafvibe.leafnotes

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Versioned, portable LeafNotes archive format.
 *
 * A .leafnotes file is UTF-8 JSON so it can be copied to a computer, inspected,
 * edited with a text editor, and imported again. Unknown future fields are ignored.
 */
internal object LeafNotesTransfer {
    private const val FORMAT = "leafnotes"
    private const val VERSION = 1

    data class Archive(
        val notes: List<Note>,
        val categories: List<NoteCategory>,
        val exportedAt: Long,
        val trash: List<TrashedNote> = emptyList(),
        val templates: List<NoteTemplate> = emptyList()
    )

    data class ImportResult(
        val notes: List<Note>,
        val categories: List<NoteCategory>,
        val addedNotes: Int,
        val skippedNotes: Int,
        val addedCategories: Int,
        val trash: List<TrashedNote> = emptyList(),
        val templates: List<NoteTemplate> = emptyList()
    )

    fun encode(notes: List<Note>, categories: List<NoteCategory>, trash: List<TrashedNote> = emptyList(), templates: List<NoteTemplate> = emptyList()): ByteArray {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("notes", JSONArray().apply {
                notes.forEach { note ->
                    put(JSONObject()
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
            })
            .put("trash", JSONArray().apply {
                trash.forEach { entry ->
                    val note = entry.note
                    put(JSONObject().put("deletedAt", entry.deletedAt).put("note", JSONObject()
                        .put("id", note.id).put("title", note.title).put("body", note.body)
                        .put("category", note.category).put("favorite", note.favorite).put("updatedAt", note.updatedAt)
                        .put("tags", JSONArray(note.tags))
                        .put("checklist", JSONArray().apply { note.checklist.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked).put("position", it.position)) } })
                        .put("flashcards", note.flashcards.toJson())
                        .put("icon", note.icon)
                        .put("reminderAt", note.reminderAt)
                        .put("revisions", note.revisions.toJson())))
                }
            })
            .put("templates", JSONArray().apply {
                templates.forEach { template ->
                    put(JSONObject().put("id", template.id).put("name", template.name).put("title", template.title)
                        .put("body", template.body).put("category", template.category ?: JSONObject.NULL))
                }
            })
            .put("categories", JSONArray().apply {
                categories.forEach { category ->
                    put(JSONObject()
                        .put("name", category.name)
                        .put("emoji", category.emoji)
                        .put("color", category.color)
                        .put("parent", category.parent ?: JSONObject.NULL))
                }
            })
        return root.toString(2).toByteArray(StandardCharsets.UTF_8)
    }

    fun encodeZip(notes: List<Note>, categories: List<NoteCategory>, trash: List<TrashedNote> = emptyList(), templates: List<NoteTemplate> = emptyList()): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("library.leafnotes"))
            zip.write(encode(notes, categories, trash, templates))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("notes.txt"))
            zip.write(exportText(notes))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): Archive {
        require(bytes.size <= 50 * 1024 * 1024) { "Archive trop volumineuse (50 Mo maximum)." }
        val payload = if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            var archiveBytes: ByteArray? = null
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name == "library.leafnotes") {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 50 * 1024 * 1024) { "Contenu ZIP trop volumineux." }
                            output.write(buffer, 0, count)
                        }
                        archiveBytes = output.toByteArray()
                        break
                    }
                }
            }
            archiveBytes ?: error("Le ZIP ne contient pas library.leafnotes.")
        } else bytes
        val root = JSONObject(String(payload, StandardCharsets.UTF_8))
        require(root.optString("format") == FORMAT) { "Ce fichier n'est pas une archive LeafNotes." }
        val version = root.optInt("version", -1)
        require(version in 1..VERSION) { "Version d'archive non prise en charge : $version." }

        val notesJson = root.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until notesJson.length()).mapNotNull { index ->
            val item = notesJson.optJSONObject(index) ?: return@mapNotNull null
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

        val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
        val categories = (0 until categoriesJson.length()).mapNotNull { index ->
            val item = categoriesJson.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            NoteCategory(
                name = name,
                emoji = item.optString("emoji", "📁"),
                color = item.optLong("color", 0xFF6C9B80),
                parent = item.optString("parent").takeIf { it.isNotBlank() && it != "null" }
            )
        }
        val trashJson = root.optJSONArray("trash") ?: JSONArray()
        val trash = (0 until trashJson.length()).mapNotNull { index ->
            val entry = trashJson.optJSONObject(index) ?: return@mapNotNull null
            val item = entry.optJSONObject("note") ?: return@mapNotNull null
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TrashedNote(
                note = Note(
                    id = id, title = item.optString("title"), body = item.optString("body"),
                    category = item.optString("category", "Nouveaux"),
                    favorite = item.optBoolean("favorite", false),
                    updatedAt = item.optLong("updatedAt", 0L),
                    tags = item.optJSONArray("tags").toStringList(),
                    checklist = item.optJSONArray("checklist").toChecklist(),
                    flashcards = item.optJSONArray("flashcards").toFlashcards(),
                    icon = item.optString("icon", "📝"),
                    reminderAt = item.optLong("reminderAt", 0L),
                    revisions = item.optJSONArray("revisions").toRevisions(id)
                ),
                deletedAt = entry.optLong("deletedAt", 0L)
            )
        }
        val templatesJson = root.optJSONArray("templates") ?: JSONArray()
        val templates = (0 until templatesJson.length()).mapNotNull { index ->
            val item = templatesJson.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").ifBlank { return@mapNotNull null }
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            NoteTemplate(id, name, item.optString("title"), item.optString("body"),
                item.optString("category").takeIf { it.isNotBlank() && it != "null" })
        }
        return Archive(notes, categories, root.optLong("exportedAt", 0L), trash, templates)
    }

    /**
     * Safe merge: existing notes are never overwritten. IDs already present are
     * skipped; category name collisions are resolved with a readable suffix.
     */
    fun merge(
        currentNotes: List<Note>,
        currentCategories: List<NoteCategory>,
        archive: Archive,
        currentTrash: List<TrashedNote> = emptyList(),
        currentTemplates: List<NoteTemplate> = emptyList()
    ): ImportResult {
        val existingIds = currentNotes.mapTo(mutableSetOf()) { it.id }
        val incomingNotes = archive.notes.filter { existingIds.add(it.id) }
        val skipped = archive.notes.size - incomingNotes.size

        val names = currentCategories.mapTo(mutableSetOf()) { it.name.lowercase() }
        val categoryMap = linkedMapOf<String, String>()
        archive.categories.forEach { category ->
            val original = category.name
            var candidate = original
            var suffix = 2
            while (!names.add(candidate.lowercase())) {
                candidate = "$original ($suffix)"
                suffix++
            }
            categoryMap[original] = candidate
        }
        val incomingCategories = archive.categories.map { category ->
            category.copy(
                name = categoryMap[category.name] ?: category.name,
                parent = category.parent?.let { categoryMap[it] ?: it }
            )
        }
        val remappedNotes = incomingNotes.map { note ->
            note.copy(category = categoryMap[note.category] ?: note.category)
        }
        val knownCategoryNames = (currentCategories + incomingCategories).mapTo(mutableSetOf()) { it.name.lowercase() }
        val missingCategories = remappedNotes.map { it.category }.distinctBy { it.lowercase() }
            .filter { knownCategoryNames.add(it.lowercase()) }
            .map { NoteCategory(it, "📁", 0xFF6C9B80) }
        val allImportedCategories = incomingCategories + missingCategories
        return ImportResult(
            notes = currentNotes + remappedNotes,
            categories = currentCategories + allImportedCategories,
            addedNotes = remappedNotes.size,
            skippedNotes = skipped,
            addedCategories = allImportedCategories.size,
            trash = currentTrash + archive.trash.filterNot { incoming -> currentTrash.any { it.note.id == incoming.note.id } },
            templates = currentTemplates + archive.templates
                .map { template -> template.copy(category = template.category?.let { categoryMap[it] ?: it }) }
                .filterNot { incoming -> currentTemplates.any { it.id == incoming.id } }
        )
    }

    fun exportText(notes: List<Note>): ByteArray {
        val text = notes.joinToString("\n\n----------------------------------------\n\n") { note ->
            buildString {
                appendLine(note.title.ifBlank { "Sans titre" })
                appendLine("Catégorie : ${note.category}")
                appendLine("Modifiée : ${note.updatedAt}")
                appendLine("Icône : ${note.icon}")
                if (note.tags.isNotEmpty()) appendLine("Tags : ${note.tags.joinToString(", ")}")
                appendLine()
                append(note.body)
                if (note.checklist.isNotEmpty()) {
                    appendLine()
                    appendLine("Liste à cocher :")
                    note.checklist.forEach { appendLine((if (it.checked) "[x] " else "[ ] ") + it.text) }
                }
                if (note.flashcards.isNotEmpty()) {
                    appendLine()
                    appendLine("Questions / réponses :")
                    note.flashcards.forEach { appendLine("Q: " + it.question); appendLine("R: " + it.answer) }
                }
            }
        }
        return text.toByteArray(StandardCharsets.UTF_8)
    }
}

/** Soft-delete record. Keep the original note intact so it can be restored. */
internal data class TrashedNote(
    val note: Note,
    val deletedAt: Long = System.currentTimeMillis()
)

internal object LeafNotesTrash {
    fun moveToTrash(notes: List<Note>, trash: List<TrashedNote>, id: String): Pair<List<Note>, List<TrashedNote>> {
        val target = notes.firstOrNull { it.id == id } ?: return notes to trash
        return notes.filterNot { it.id == id } to (trash.filterNot { it.note.id == id } + TrashedNote(target))
    }

    fun restore(trash: List<TrashedNote>, id: String, notes: List<Note>): Pair<List<Note>, List<TrashedNote>> {
        val item = trash.firstOrNull { it.note.id == id } ?: return notes to trash
        if (notes.any { it.id == id }) return notes to trash
        return (notes + item.note) to trash.filterNot { it.note.id == id }
    }

    fun permanentlyDelete(trash: List<TrashedNote>, id: String): List<TrashedNote> =
        trash.filterNot { it.note.id == id }
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

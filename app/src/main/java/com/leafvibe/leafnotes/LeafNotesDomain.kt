package com.leafvibe.leafnotes

import java.util.UUID

/**
 * Rich-editor domain model. These models deliberately stay independent from Compose
 * so the editor, search, export and future desktop companion can share the same data.
 */
internal data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean = false,
    val position: Int = 0
)

internal data class NoteAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val mimeType: String,
    val displayName: String,
    val sizeBytes: Long = 0L
)

internal data class NoteReminder(
    val id: String = UUID.randomUUID().toString(),
    val triggerAt: Long,
    val repeatRule: String? = null,
    val enabled: Boolean = true
)

internal data class NoteTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val title: String,
    val body: String,
    val category: String? = null
)

internal data class NoteRevision(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val title: String,
    val body: String,
    val savedAt: Long = System.currentTimeMillis()
)

internal object LeafNotesOrganization {
    fun normalizeTags(tags: List<String>): List<String> =
        tags.map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

    fun searchMatches(note: Note, query: String, tags: List<String> = emptyList()): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return note.title.contains(q, ignoreCase = true) ||
            note.body.contains(q, ignoreCase = true) ||
            note.category.contains(q, ignoreCase = true) ||
            tags.any { it.contains(q, ignoreCase = true) } ||
            note.checklist.any { it.text.contains(q, ignoreCase = true) }
    }

    fun moveNote(note: Note, destinationCategory: String): Note =
        note.copy(category = destinationCategory, updatedAt = System.currentTimeMillis())

    fun validateCategoryMove(
        categoryName: String,
        newParent: String?,
        categories: List<NoteCategory>
    ): String? {
        if (newParent == categoryName) return "Un dossier ne peut pas être son propre parent."
        if (newParent != null && categories.none { it.name == newParent })
            return "Le dossier parent demandé n'existe pas."
        val byName = categories.associateBy { it.name }
        var cursor = newParent
        val visited = mutableSetOf(categoryName)
        while (cursor != null) {
            if (!visited.add(cursor)) return "Cette opération créerait une boucle dans les dossiers."
            cursor = byName[cursor]?.parent
        }
        return null
    }

    fun descendants(categoryName: String, categories: List<NoteCategory>): Set<String> {
        val result = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(categoryName)
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            categories.filter { it.parent == parent }.forEach { child ->
                if (result.add(child.name)) queue.add(child.name)
            }
        }
        return result
    }
}

/** Small in-memory undo/redo primitive; persistence belongs to the repository layer. */
internal class LeafNotesHistory<T>(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    fun record(current: T) {
        undoStack.addLast(current)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: T): T? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    fun redo(current: T): T? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        return redoStack.removeLast()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}

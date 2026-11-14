package com.leafvibe.leafnotes

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeafNotesCoreInstrumentedTest {

    @Test
    fun archiveRoundTripPreservesNotesAndCategoryHierarchy() {
        val categories = listOf(
            NoteCategory("Cours", "📚", 0xFF5685C5),
            NoteCategory("Maths", "➗", 0xFF4D9B83, "Cours")
        )
        val notes = listOf(
            Note(id = "note-1", title = "Matrices", body = "Cours BTS", category = "Maths", favorite = true, updatedAt = 1234L)
        )

        val decoded = LeafNotesTransfer.decode(LeafNotesTransfer.encode(notes, categories))

        assertEquals(notes, decoded.notes)
        assertEquals(categories, decoded.categories)
    }

    @Test
    fun importMergeSkipsExistingIdsAndRenamesCategoryCollisions() {
        val existing = listOf(Note(id = "same", title = "Local", body = "Keep", category = "Cours"))
        val incoming = LeafNotesTransfer.Archive(
            notes = listOf(
                Note(id = "same", title = "Imported duplicate", body = "Must not replace", category = "Cours"),
                Note(id = "new", title = "New", body = "Imported", category = "Cours")
            ),
            categories = listOf(NoteCategory("Cours", "📚", 0xFF5685C5)),
            exportedAt = 1L
        )

        val result = LeafNotesTransfer.merge(existing, listOf(NoteCategory("Cours", "📖", 0xFF000000)), incoming)

        assertEquals("Local", result.notes.first().title)
        assertEquals(1, result.addedNotes)
        assertEquals(1, result.skippedNotes)
        assertEquals("Cours (2)", result.categories.last().name)
        assertEquals("Cours (2)", result.notes.last().category)
    }

    @Test
    fun trashCanRestoreAndPermanentlyDeleteNotes() {
        val note = Note(id = "trash-me", title = "À restaurer", body = "Texte", category = "Idées")
        val (remaining, trash) = LeafNotesTrash.moveToTrash(listOf(note), emptyList(), note.id)

        assertTrue(remaining.isEmpty())
        assertEquals(1, trash.size)

        val (restored, emptiedTrash) = LeafNotesTrash.restore(trash, note.id, remaining)
        assertEquals(listOf(note), restored)
        assertTrue(emptiedTrash.isEmpty())

        val permanentlyDeleted = LeafNotesTrash.permanentlyDelete(trash, note.id)
        assertTrue(permanentlyDeleted.isEmpty())
    }

    @Test
    fun organizationSearchAndFolderDescendantsWork() {
        val categories = listOf(
            NoteCategory("Cours", "📚", 1L),
            NoteCategory("Maths", "➗", 1L, "Cours"),
            NoteCategory("Matrices", "🔢", 1L, "Maths")
        )
        val note = Note(id = "n", title = "Algèbre", body = "Matrice identité", category = "Matrices")

        assertTrue(LeafNotesOrganization.searchMatches(note, "identité"))
        assertTrue(LeafNotesOrganization.descendants("Cours", categories).containsAll(listOf("Maths", "Matrices")))
        assertFalse(LeafNotesOrganization.searchMatches(note, "géographie"))
        assertNotNull(LeafNotesOrganization.validateCategoryMove("Cours", "Matrices", categories))
    }
}

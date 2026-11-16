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
    fun portableArchivePreservesAttachmentsAndNestedNoteParent() {
        val parent = Note(id = "parent", title = "Dossier", body = "", category = "Cours")
        val child = Note(
            id = "child",
            title = "Vidéo de cours",
            body = "Voir la pièce jointe",
            category = "Cours",
            attachments = listOf(NoteAttachment(id = "media-1", uri = "content://media/video/1", mimeType = "video/mp4", displayName = "cours.mp4", sizeBytes = 1234L)),
            parentNoteId = parent.id
        )
        val archive = LeafNotesTransfer.decode(
            LeafNotesTransfer.encode(listOf(parent, child), listOf(NoteCategory("Cours", "📚", 1L)))
        )
        val restored = archive.notes.single { it.id == child.id }
        assertEquals(parent.id, restored.parentNoteId)
        assertEquals("cours.mp4", restored.attachments.single().displayName)
        assertEquals("video/mp4", restored.attachments.single().mimeType)
        assertEquals(1234L, restored.attachments.single().sizeBytes)
    }

    @Test
    fun archiveRoundTripPreservesNotesAndCategoryHierarchy() {
        val categories = listOf(
            NoteCategory("Cours", "📚", 0xFF5685C5),
            NoteCategory("Maths", "➗", 0xFF4D9B83, "Cours")
        )
        val notes = listOf(
            Note(id = "note-1", title = "Matrices", body = "Cours BTS", category = "Maths", favorite = true, updatedAt = 1234L, tags = listOf("BTS", "révision"), checklist = listOf(ChecklistItem(id = "c1", text = "Apprendre", checked = true, position = 0)), flashcards = listOf(Flashcard(id = "f1", question = "Q", answer = "A", dueAt = 123L, intervalDays = 3, repetitions = 2)), icon = "🧠", reminderAt = 4567L, revisions = listOf(NoteRevision(id = "rev1", noteId = "note-1", title = "Ancien titre", body = "Ancien contenu", savedAt = 99L)))
        )

        val trashed = listOf(TrashedNote(notes.first(), deletedAt = 2222L))
        val templates = listOf(NoteTemplate(id = "template-1", name = "Fiche de cours", title = "Titre", body = "Plan", category = "Cours"))
        val decoded = LeafNotesTransfer.decode(LeafNotesTransfer.encode(notes, categories, trashed, templates))

        assertEquals(notes, decoded.notes)
        assertEquals(categories, decoded.categories)
        assertEquals(trashed, decoded.trash)
        assertEquals(templates, decoded.templates)
    }

    @Test
    fun zipBackupRoundTripPreservesLibraryAndTemplates() {
        val note = Note(id = "zip-note", title = "ZIP", body = "Portable", category = "Cours", icon = "🧩", reminderAt = 1234L)
        val categories = listOf(NoteCategory("Cours", "📚", 0xFF5685C5))
        val templates = listOf(NoteTemplate(id = "zip-template", name = "Compte rendu", title = "Réunion", body = "Actions", category = "Cours"))
        val decoded = LeafNotesTransfer.decode(LeafNotesTransfer.encodeZip(listOf(note), categories, templates = templates))
        assertEquals(listOf(note), decoded.notes)
        assertEquals(categories, decoded.categories)
        assertEquals(templates, decoded.templates)
    }

    @Test
    fun importMergeSkipsExistingIdsAndRenamesCategoryCollisions() {
        val existing = listOf(Note(id = "same", title = "Local", body = "Keep", category = "Cours"))
        val incoming = LeafNotesTransfer.Archive(
            notes = listOf(
                Note(id = "same", title = "Imported duplicate", body = "Must not replace", category = "Cours"),
                Note(id = "new", title = "New", body = "Imported", category = "Cours")
            ),
            categories = listOf(NoteCategory("Cours", "📚", 0xFF5685C5), NoteCategory("Sous-cours", "🗂️", 0xFF5685C5, "Cours")),
            exportedAt = 1L
        )

        val result = LeafNotesTransfer.merge(existing, listOf(NoteCategory("Cours", "📖", 0xFF000000)), incoming)

        assertEquals("Local", result.notes.first().title)
        assertEquals(1, result.addedNotes)
        assertEquals(1, result.skippedNotes)
        assertTrue(result.categories.any { it.name == "Cours (2)" })
        assertEquals("Cours (2)", result.notes.last().category)
        assertEquals("Cours (2)", result.categories.single { it.name == "Sous-cours" }.parent)
    }

    @Test
    fun importRemapsTrashedNoteCategoryWhenNameCollides() {
        val archivedNote = Note(id = "trash-source", title = "Ancienne note", body = "Texte", category = "Cours")
        val archive = LeafNotesTransfer.Archive(
            notes = emptyList(),
            categories = listOf(NoteCategory("Cours", "📚", 0xFF5685C5)),
            exportedAt = 20L,
            trash = listOf(TrashedNote(archivedNote, deletedAt = 21L))
        )
        val result = LeafNotesTransfer.merge(emptyList(), listOf(NoteCategory("Cours", "📖", 0xFF000000)), archive)
        assertEquals("Cours (2)", result.trash.single().note.category)
        assertTrue(result.categories.any { it.name == "Cours (2)" })
    }

    @Test
    fun importRecreatesCategoryReferencedByNoteWhenArchiveOmitsIt() {
        val archive = LeafNotesTransfer.Archive(
            notes = listOf(Note(id = "orphan", title = "Note importée", body = "Texte", category = "Catégorie absente")),
            categories = emptyList(),
            exportedAt = 10L
        )
        val result = LeafNotesTransfer.merge(emptyList(), listOf(NoteCategory("Cours", "📚", 1L)), archive)
        assertEquals("Catégorie absente", result.notes.single().category)
        assertTrue(result.categories.any { it.name == "Catégorie absente" })
        assertEquals(1, result.addedCategories)
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

    @Test
    fun localStorePersistsNotesCategoriesAndTrashAcrossInstances() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("leafnotes-test-store", android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val firstStore = LeafNotesStore(preferences)
        val note = Note(id = "persisted", title = "Test durable", body = "Contenu", category = "Maths", favorite = true, updatedAt = 99L, tags = listOf("BTS"), checklist = listOf(ChecklistItem(id = "check1", text = "Exercice", checked = false, position = 0)), flashcards = listOf(Flashcard(id = "f2", question = "Pourquoi ?", answer = "Parce que", dueAt = 456L, intervalDays = 1, repetitions = 1)), icon = "📌", reminderAt = 9876L, revisions = listOf(NoteRevision(id = "rev2", noteId = "persisted", title = "Version précédente", body = "Ancien texte", savedAt = 88L)))
        val categories = listOf(
            NoteCategory("Cours", "📚", 1L),
            NoteCategory("Maths", "➗", 2L, "Cours")
        )
        firstStore.saveNotes(listOf(note))
        firstStore.saveCategories(categories)
        firstStore.saveTrash(listOf(TrashedNote(note, deletedAt = 123L)))
        val templates = listOf(NoteTemplate(id = "template-store", name = "Compte rendu", title = "Réunion", body = "Décisions", category = "Cours"))
        firstStore.saveTemplates(templates)

        val reopenedStore = LeafNotesStore(preferences)
        assertEquals(listOf(note), reopenedStore.loadNotes())
        assertEquals(categories, reopenedStore.loadCategories(emptyList()))
        assertEquals(listOf(TrashedNote(note, deletedAt = 123L)), reopenedStore.loadTrash())
        assertEquals(templates, reopenedStore.loadTemplates())
        preferences.edit().clear().commit()
    }

}

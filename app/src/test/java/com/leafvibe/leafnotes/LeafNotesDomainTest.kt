package com.leafvibe.leafnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafNotesDomainTest {
    @Test
    fun tagsAreTrimmedDeduplicatedAndNormalized() {
        assertEquals(
            listOf("BTS", "Maths"),
            LeafNotesOrganization.normalizeTags(listOf(" Maths ", "#BTS", "maths", "#maths", ""))
        )
    }

    @Test
    fun searchIncludesTitleBodyCategoryAndTags() {
        val note = Note(id = "1", title = "Matrices", body = "Algèbre linéaire", category = "Cours", tags = listOf("BTS", "révision"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "matrices"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "algèbre"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "cours"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "BTS", note.tags))
        assertTrue(LeafNotesOrganization.searchMatches(note, "révision", note.tags))
        assertFalse(LeafNotesOrganization.searchMatches(note, "physique"))
    }

    @Test
    fun tagsAreNormalizedAndCaseInsensitiveWithoutDuplicates() {
        assertEquals(listOf("BTS", "Maths"), LeafNotesOrganization.normalizeTags(listOf(" Maths ", "#BTS", "maths", "#maths", "")))
    }

    @Test
    fun spacedRepetitionSchedulesReviewAndMastery() {
        val card = Flashcard(id = "q1", question = "2 + 2 ?", answer = "4")
        val retry = LeafQuizScheduling.review(card, remembered = false, now = 1_000L)
        assertEquals(601_000L, retry.dueAt)
        assertEquals(0, retry.repetitions)
        val mastered = LeafQuizScheduling.review(card, remembered = true, now = 1_000L)
        assertEquals(86_401_000L, mastered.dueAt)
        assertEquals(1, mastered.intervalDays)
        assertEquals(1, mastered.repetitions)
    }

    @Test
    fun exportTextContainsTagsChecklistAndQuizContent() {
        val note = Note(
            id = "export",
            title = "Révisions",
            body = "Contenu",
            category = "Cours",
            tags = listOf("BTS"),
            checklist = listOf(ChecklistItem(id = "task", text = "Apprendre", checked = true)),
            flashcards = listOf(Flashcard(id = "card", question = "Question ?", answer = "Réponse"))
        )
        val exported = String(LeafNotesTransfer.exportText(listOf(note)), Charsets.UTF_8)
        assertTrue(exported.contains("Tags : BTS"))
        assertTrue(exported.contains("[x] Apprendre"))
        assertTrue(exported.contains("Q: Question ?"))
    }

    @Test
    fun documentExportsContainNoteContentInPortableFormats() {
        val note = Note(id = "export-2", title = "Idées", body = "Une ligne\nDeux lignes", category = "Projet")
        val markdown = String(LeafNotesTransfer.exportMarkdown(listOf(note)), Charsets.UTF_8)
        val csv = String(LeafNotesTransfer.exportCsv(listOf(note)), Charsets.UTF_8)
        val html = String(LeafNotesTransfer.exportHtml(listOf(note)), Charsets.UTF_8)
        assertTrue(markdown.contains("# Idées"))
        assertTrue(csv.contains("\"Projet\""))
        assertTrue(html.contains("<h1>LeafNotes</h1>"))
        assertTrue(html.contains("Deux lignes"))
    }

    @Test
    fun suggestionsAreDeterministicAndRemainLocal() {
        val categories = listOf(
            NoteCategory("Cours de maths", "📚", 1L),
            NoteCategory("Cuisine", "🍲", 2L)
        )
        val result = LeafNotesSuggestions.suggest("Matrices BTS", "Ce cours de maths présente les matrices pour les calculs algébriques. Matrices et algèbre.", categories)
        assertEquals("Cours de maths", result.category)
        assertTrue(result.tags.contains("matrices"))
        assertTrue(result.tags.contains("algébriques"))
    }

    @Test
    fun deletingCustomRootCategoryMovesNotesAndPromotesChildren() {
        val notes = listOf(Note(id = "n1", title = "Note", body = "", category = "Projet"))
        val categories = listOf(
            NoteCategory("Projet", "📁", 1L),
            NoteCategory("Sous-dossier", "📂", 2L, "Projet"),
            NoteCategory("Nouveaux", "✨", 3L)
        )
        val result = LeafNotesOrganization.deleteCustomCategory("Projet", notes, categories)
        assertEquals("Nouveaux", result?.notes?.single()?.category)
        assertEquals(null, result?.categories?.single { it.name == "Sous-dossier" }?.parent)
        assertTrue(LeafNotesOrganization.deleteCustomCategory("Cours", notes, categories) == null)
    }

    @Test
    fun categoryMoveRejectsSelfParentAndCycles() {
        val categories = listOf(
            NoteCategory("Cours", "📚", 1L),
            NoteCategory("Maths", "➗", 1L, "Cours"),
            NoteCategory("Matrices", "🔢", 1L, "Maths")
        )
        assertEquals(null, LeafNotesOrganization.validateCategoryMove("Matrices", "Cours", categories))
        assertTrue(LeafNotesOrganization.validateCategoryMove("Cours", "Matrices", categories) != null)
        assertTrue(LeafNotesOrganization.validateCategoryMove("Cours", "Cours", categories) != null)
    }
}

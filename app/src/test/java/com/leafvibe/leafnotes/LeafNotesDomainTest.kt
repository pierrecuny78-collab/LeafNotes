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

package com.leafvibe.leafnotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafNotesDomainTest {
    @Test
    fun tagsAreTrimmedDeduplicatedAndNormalized() {
        assertEquals(
            listOf("bts", "maths"),
            LeafNotesOrganization.normalizeTags(listOf(" Maths ", "#BTS", "maths", "#maths", ""))
        )
    }

    @Test
    fun searchIncludesTitleBodyCategoryAndTags() {
        val note = Note(id = "1", title = "Matrices", body = "Algèbre linéaire", category = "Cours")
        assertTrue(LeafNotesOrganization.searchMatches(note, "matrices"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "algèbre"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "cours"))
        assertTrue(LeafNotesOrganization.searchMatches(note, "BTS", listOf("BTS")))
        assertFalse(LeafNotesOrganization.searchMatches(note, "physique"))
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

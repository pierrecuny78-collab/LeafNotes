package com.leafvibe.leafnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LeafNotesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenDisplaysLibraryAndPrimaryActions() {
        composeRule.onNodeWithText("LeafNotes").assertIsDisplayed()
        composeRule.onNodeWithText("Catégories").assertIsDisplayed()
        composeRule.onNodeWithText("Outils").assertIsDisplayed()
    }

    @Test
    fun noteEditorProvidesTagsAndChecklistControls() {
        composeRule.onNodeWithContentDescription("Nouvelle note").performClick()
        composeRule.onNodeWithText("Étiquettes (séparées par des virgules)").assertIsDisplayed()
        composeRule.onNodeWithText("Liste à cocher").assertIsDisplayed()
        composeRule.onNodeWithText("Nouvel élément").assertIsDisplayed()
        composeRule.onNodeWithText("Questions / réponses pour LeafQuiz").assertIsDisplayed()
    }

    @Test
    fun leafQuizCanBeOpenedFromTheLibrary() {
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("LeafQuiz").assertIsDisplayed()
        composeRule.onNodeWithText("Aucune question pour le moment").assertIsDisplayed()
    }

    @Test
    fun toolsScreenExposesBackupImportAndTrash() {
        composeRule.onNodeWithText("Outils").performClick()
        composeRule.onNodeWithText("Outils et sauvegardes").assertIsDisplayed()
        composeRule.onNodeWithText("Exporter une sauvegarde").assertIsDisplayed()
        composeRule.onNodeWithText("Importer une sauvegarde").assertIsDisplayed()
        composeRule.onNodeWithText("Corbeille (0)").assertIsDisplayed()
    }
}

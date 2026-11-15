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
        composeRule.onNodeWithText("LeafBackup").assertIsDisplayed()
        composeRule.onNodeWithText("LeafSwipe").assertIsDisplayed()
        composeRule.onNodeWithText("LeafLearning").assertIsDisplayed()
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
    fun graphOffersRequestedCustomizationControls() {
        composeRule.onNodeWithText("🕸️ Graph").performClick()
        composeRule.onNodeWithText("Crème").assertIsDisplayed()
        composeRule.onNodeWithText("Nuit").assertIsDisplayed()
        composeRule.onNodeWithText("Sauge").assertIsDisplayed()
        composeRule.onNodeWithText("Papier").assertIsDisplayed()
        composeRule.onNodeWithText("Losange").assertIsDisplayed()
    }

    @Test
    fun leafQuizCanBeOpenedFromTheLibrary() {
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("LeafQuiz").assertIsDisplayed()
        composeRule.onNodeWithText("Aucune question pour le moment").assertIsDisplayed()
    }

    @Test
    fun swipeAndLearningModulesOpen() {
        composeRule.onNodeWithText("LeafSwipe").performClick()
        composeRule.onNodeWithText("LeafSwipe · Révision rapide").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Retour").performClick()
        composeRule.onNodeWithText("LeafLearning").performClick()
        composeRule.onNodeWithText("LeafLearning · Apprendre activement").assertIsDisplayed()
    }

    @Test
    fun settingsPanelExplainsModulesAndOpensBackup() {
        composeRule.onNodeWithContentDescription("Paramètres et aide").performClick()
        composeRule.onNodeWithText("Paramètres et aide LeafNotes").assertIsDisplayed()
        composeRule.onNodeWithText("Ouvrir LeafBackup").performClick()
        composeRule.onNodeWithText("Outils et sauvegardes").assertIsDisplayed()
    }

    @Test
    fun toolsScreenExposesBackupImportAndTrash() {
        composeRule.onNodeWithText("LeafBackup").performClick()
        composeRule.onNodeWithText("Outils et sauvegardes").assertIsDisplayed()
        composeRule.onNodeWithText("Exporter une sauvegarde").assertIsDisplayed()
        composeRule.onNodeWithText("Importer une sauvegarde").assertIsDisplayed()
        composeRule.onNodeWithText("Corbeille (0)").assertIsDisplayed()
    }
}

package com.leafvibe.leafnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeafQuizScreen(
    notes: List<Note>,
    onUpdateNote: (Note) -> Unit,
    onClose: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val allCards = notes.flatMap { note -> note.flashcards.map { note to it } }
    val dueCards = allCards.filter { it.second.dueAt <= now }
    val current = dueCards.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LeafQuiz") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour à LeafNotes")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${dueCards.size} carte(s) à réviser • ${allCards.size} au total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (current == null) {
                Spacer(Modifier.height(40.dp))
                Text("🎉", style = MaterialTheme.typography.displayMedium)
                Text(
                    if (allCards.isEmpty()) "Aucune question pour le moment" else "Toutes les cartes sont à jour",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (allCards.isEmpty()) "Ouvre une note et ajoute des questions/réponses pour commencer."
                    else "Tes prochaines révisions apparaîtront automatiquement ici.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(current.first.title.ifBlank { "Note sans titre" }, style = MaterialTheme.typography.labelLarge)
                Card(
                    Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("QUESTION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(current.second.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        if (revealed) {
                            Spacer(Modifier.height(28.dp))
                            Text("RÉPONSE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(current.second.answer, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (!revealed) {
                    Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Afficher la réponse")
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val updated = LeafQuizScheduling.review(current.second, remembered = false)
                                onUpdateNote(current.first.copy(flashcards = current.first.flashcards.map { if (it.id == current.second.id) updated else it }))
                                revealed = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("À revoir") }
                        Button(
                            onClick = {
                                val updated = LeafQuizScheduling.review(current.second, remembered = true)
                                onUpdateNote(current.first.copy(flashcards = current.first.flashcards.map { if (it.id == current.second.id) updated else it }))
                                revealed = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Acquis") }
                    }
                }
            }
        }
    }
}

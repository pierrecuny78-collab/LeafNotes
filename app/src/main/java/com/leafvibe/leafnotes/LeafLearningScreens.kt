package com.leafvibe.leafnotes

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LeafSwipeScreen(notes: List<Note>, onOpenNote: (Note) -> Unit, onClose: () -> Unit) {
    var index by remember(notes) { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var drag by remember { mutableFloatStateOf(0f) }
    val currentIndex = index.coerceIn(0, (notes.size - 1).coerceAtLeast(0))
    val note = notes.getOrNull(currentIndex)

    Scaffold(topBar = {
        TopAppBar(title = { Text("LeafSwipe · Révision rapide") }, navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Retour") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (notes.isEmpty()) "0 / 0" else "${currentIndex + 1} / ${notes.size}", style = MaterialTheme.typography.labelLarge)
            if (note == null) {
                Text("Ajoute des notes pour commencer.", style = MaterialTheme.typography.titleMedium)
            } else {
                Card(
                    Modifier.weight(1f).fillMaxWidth().pointerInput(note.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (abs(drag) > 55f) {
                                    index = (index + if (drag < 0f) 1 else -1).coerceIn(0, notes.lastIndex)
                                    revealed = false
                                }
                                drag = 0f
                            },
                            onHorizontalDrag = { _, amount -> drag += amount }
                        )
                    }
                ) {
                    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(note.icon, style = MaterialTheme.typography.displaySmall)
                        Text(note.title.ifBlank { "Sans titre" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(note.category, color = MaterialTheme.colorScheme.primary)
                        if (revealed) {
                            Spacer(Modifier.height(20.dp))
                            Text(note.body.ifBlank { "Cette note ne contient pas encore de réponse." })
                        } else {
                            Spacer(Modifier.height(20.dp))
                            Text("Essaie de te rappeler le contenu avant de le révéler.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (!revealed) Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("Révéler") }
                else Button(onClick = { index = (index + 1).coerceAtMost(notes.lastIndex); revealed = false }, modifier = Modifier.fillMaxWidth()) { Text("Carte suivante") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { index = (index - 1).coerceAtLeast(0); revealed = false }, modifier = Modifier.weight(1f), enabled = currentIndex > 0) { Text("Précédente") }
                    OutlinedButton(onClick = { onOpenNote(note) }, modifier = Modifier.weight(1f)) { Text("Modifier la note") }
                    OutlinedButton(onClick = { index = (index + 1).coerceAtMost(notes.lastIndex); revealed = false }, modifier = Modifier.weight(1f), enabled = currentIndex < notes.lastIndex) { Text("Suivante") }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LeafLearningScreen(notes: List<Note>, onClose: () -> Unit) {
    var index by remember(notes) { mutableIntStateOf(0) }
    var reformulation by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var mastered by remember { mutableIntStateOf(0) }
    var review by remember { mutableIntStateOf(0) }
    val currentIndex = index.coerceIn(0, (notes.size - 1).coerceAtLeast(0))
    val note = notes.getOrNull(currentIndex)

    Scaffold(topBar = {
        TopAppBar(title = { Text("LeafLearning · Apprendre activement") }, navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Retour") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Maîtrisées : $mastered • À revoir : $review", style = MaterialTheme.typography.labelLarge)
            if (note == null) Text("Ajoute des notes pour commencer.")
            else {
                Text("Fiche ${currentIndex + 1} / ${notes.size} • ${note.category}", color = MaterialTheme.colorScheme.primary)
                Text(note.title.ifBlank { "Sans titre" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(reformulation, { reformulation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Reformule avec tes mots") }, minLines = 3)
                if (revealed) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Contenu de référence", fontWeight = FontWeight.SemiBold)
                            Text(note.body.ifBlank { "Aucun contenu enregistré." })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { review++; index = (index + 1).coerceAtMost(notes.lastIndex); reformulation = ""; revealed = false }, modifier = Modifier.weight(1f)) { Text("À revoir") }
                        Button(onClick = { mastered++; index = (index + 1).coerceAtMost(notes.lastIndex); reformulation = ""; revealed = false }, modifier = Modifier.weight(1f)) { Text("Maîtrisée") }
                    }
                } else {
                    Button(onClick = { revealed = true }, enabled = reformulation.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Vérifier ma reformulation") }
                }
            }
        }
    }
}

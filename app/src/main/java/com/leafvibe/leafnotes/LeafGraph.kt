package com.leafvibe.leafnotes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class GraphGroup(
    val category: NoteCategory?,
    val notes: List<Note>,
    val childCount: Int = 0
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LeafGraphScreen(
    notes: List<Note>,
    categories: List<NoteCategory>,
    onClose: () -> Unit,
    onOpenNote: (Note) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showEmpty by remember { mutableStateOf(true) }

    val filteredNotes = notes.filter {
        query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) || it.category.contains(query, true)
    }
    val groups = categories
        .sortedWith(compareBy<NoteCategory> { it.parent ?: "" }.thenBy { it.name.lowercase() })
        .map { category ->
            GraphGroup(
                category = category,
                notes = filteredNotes.filter { it.category == category.name },
                childCount = categories.count { it.parent == category.name }
            )
        }
        .filter { showEmpty || it.notes.isNotEmpty() || it.childCount > 0 } +
        listOfNotNull(
            filteredNotes.filter { note -> categories.none { it.name == note.category } }
                .takeIf { it.isNotEmpty() }
                ?.let { GraphGroup(null, it) }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LeafGraph") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour à LeafNotes")
                    }
                },
                actions = {
                    TextButton(onClick = { showEmpty = !showEmpty }) {
                        Text(if (showEmpty) "Masquer vides" else "Tout afficher")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Carte de ta bibliothèque",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${filteredNotes.size} note(s) • ${categories.size} catégorie(s) • relations par classement",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Filtrer les éléments du graphe") }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = showEmpty, onClick = { showEmpty = !showEmpty }, label = { Text("Afficher les catégories vides") })
            }
            if (groups.isEmpty()) {
                Text("Aucun élément à représenter pour le moment.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups, key = { it.category?.name ?: "__uncategorized__" }) { group ->
                        GraphGroupCard(group = group, onOpenNote = onOpenNote)
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphGroupCard(group: GraphGroup, onOpenNote: (Note) -> Unit) {
    val category = group.category
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category?.emoji ?: "📄", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(category?.name ?: "Sans catégorie", fontWeight = FontWeight.Bold)
                    val parentText = category?.parent?.let { "Dans : $it • " } ?: ""
                    Text(
                        parentText + "${group.notes.size} note(s)" +
                            if (group.childCount > 0) " • ${group.childCount} sous-dossier(s)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("●", color = MaterialTheme.colorScheme.primary)
            }
            if (group.notes.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("└─", color = MaterialTheme.colorScheme.primary)
                    group.notes.forEach { note ->
                        Card(onClick = { onOpenNote(note) }) {
                            Column(Modifier.width(190.dp).padding(10.dp)) {
                                Text(note.title.ifBlank { "Sans titre" }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(note.body.ifBlank { "Note vide" }, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (note.favorite) "⭐ " else "📝 ")
                                    Text("Ouvrir", style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    if (group.childCount > 0) "Ce dossier contient des sous-dossiers, mais aucune note directe."
                    else "Aucune note dans cette catégorie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

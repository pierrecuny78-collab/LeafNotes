package com.leafvibe.leafnotes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val category: String,
    val favorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
private data class NoteCategory(val name: String, val emoji: String, val color: Long)

private const val FAVORITES_FILTER = "__leafnotes_favorites_filter__"

private val starterCategories = listOf(
    NoteCategory("Nouveaux", "✨", 0xFF8E7CC3),
    NoteCategory("Favoris", "⭐", 0xFFE0A52B),
    NoteCategory("Idées", "💡", 0xFF4D9B83),
    NoteCategory("Cours", "📚", 0xFF5685C5),
    NoteCategory("Informatique", "💻", 0xFF597C91),
    NoteCategory("Archives", "🗄️", 0xFF8B8B8B)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeafNotesApp() }
    }
}

@Composable
private fun LeafNotesApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("leafnotes", Context.MODE_PRIVATE) }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var categories by remember { mutableStateOf(loadCategories(prefs)) }
    var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCategoryEditor by remember { mutableStateOf(false) }

    fun saveNotes(value: List<Note>) {
        notes = value
        prefs.edit().putString("notes", JSONArray().apply {
            value.forEach { n -> put(JSONObject().apply {
                put("id", n.id); put("title", n.title); put("body", n.body)
                put("category", n.category); put("favorite", n.favorite); put("updatedAt", n.updatedAt)
            }) }
        }.toString()).apply()
    }
    fun saveCategories(value: List<NoteCategory>) {
        categories = value
        prefs.edit().putString("categories", JSONArray().apply {
            value.forEach { c -> put(JSONObject().apply {
                put("name", c.name); put("emoji", c.emoji); put("color", c.color)
            }) }
        }.toString()).apply()
    }

    val visible = notes.filter { n ->
        val categoryMatch = when (selectedCategory) {
            "Tous" -> true
            FAVORITES_FILTER -> n.favorite
            else -> n.category == selectedCategory
        }
        categoryMatch && (query.isBlank() || n.title.contains(query, true) || n.body.contains(query, true))
    }.sortedByDescending { it.updatedAt }
    val pageCount = maxOf(1, (visible.size + 4) / 5)
    val safePage = page.coerceIn(0, pageCount - 1)
    val pageNotes = visible.drop(safePage * 5).take(5)

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle note")
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).padding(top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LeafNotes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Ton espace, tes idées.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        darkTheme = !darkTheme
                        prefs.edit().putBoolean("dark", darkTheme).apply()
                    }) { Icon(Icons.Default.DarkMode, contentDescription = "Changer le thème") }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it; page = 0 },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Rechercher dans tes notes…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedCategory == "Tous", { selectedCategory = "Tous"; page = 0 }, label = { Text("Tout") }) }
                    item { FilterChip(selectedCategory == FAVORITES_FILTER, { selectedCategory = FAVORITES_FILTER; page = 0 }, label = { Text("⭐ Favoris") }) }
                    items(categories) { category ->
                        FilterChip(selectedCategory == category.name, { selectedCategory = category.name; page = 0 },
                            label = { Text("${category.emoji} ${category.name}") })
                    }
                    item { AssistChip(onClick = { showCategoryEditor = true }, label = { Text("＋ Catégorie") }) }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when (selectedCategory) { "Tous" -> "Ma bibliothèque"; FAVORITES_FILTER -> "Favoris"; else -> selectedCategory },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Text("${visible.size} note(s)", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                if (pageNotes.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱", style = MaterialTheme.typography.displayMedium)
                            Text(if (query.isBlank()) "Aucune note ici pour le moment" else "Aucun résultat")
                            Text("Appuie sur + pour créer une note.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                        items(pageNotes, key = { it.id }) { note ->
                            NoteCard(note,
                                onEdit = { editing = note; showEditor = true },
                                onFavorite = { saveNotes(notes.map {
                                    if (it.id == note.id) it.copy(favorite = !it.favorite, updatedAt = System.currentTimeMillis()) else it
                                }) },
                                onDelete = { saveNotes(notes.filterNot { it.id == note.id }) })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { page = (safePage - 1).coerceAtLeast(0) }, enabled = safePage > 0) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Page précédente")
                    }
                    Text("${safePage + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { page = (safePage + 1).coerceAtMost(pageCount - 1) }, enabled = safePage < pageCount - 1) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Page suivante")
                    }
                }
            }
        }
    }

    if (showEditor) NoteEditorDialog(editing, categories, { showEditor = false }) { title, body, category ->
        val old = editing
        val changed = if (old == null) notes + Note(title = title, body = body, category = category)
        else notes.map { if (it.id == old.id) old.copy(title = title, body = body, category = category, updatedAt = System.currentTimeMillis()) else it }
        saveNotes(changed)
        showEditor = false
    }
    if (showCategoryEditor) CategoryEditorDialog({ showCategoryEditor = false }) { name, emoji ->
        val normalizedName = name.trim()
        if (normalizedName.isNotEmpty() &&
            !normalizedName.equals("Tous", true) &&
            categories.none { it.name.equals(normalizedName, true) }) {
            saveCategories(categories + NoteCategory(normalizedName, emoji.ifBlank { "📁" }, 0xFF6C9B80))
        }
        showCategoryEditor = false
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title.ifBlank { "Sans titre" }, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onFavorite) {
                    Icon(if (note.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (note.favorite) Color(0xFFE45B72) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
            }
            if (note.body.isNotBlank()) Text(note.body, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(initial: Note?, categories: List<NoteCategory>, onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember(initial) { mutableStateOf(initial?.body.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: categories.firstOrNull()?.name ?: "Nouveaux") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle note" else "Modifier la note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Titre") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Écris tes idées…") }, minLines = 5)
                Text("Catégorie", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { c ->
                        FilterChip(category == c.name, { category = c.name }, label = { Text("${c.emoji} ${c.name}") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title.trim(), body.trim(), category) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@Composable
private fun CategoryEditorDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Créer une catégorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, emoji) }, enabled = name.isNotBlank()) { Text("Créer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

private fun loadNotes(prefs: android.content.SharedPreferences): List<Note> = runCatching {
    val array = JSONArray(prefs.getString("notes", "[]"))
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Note(o.optString("id"), o.optString("title"), o.optString("body"),
            o.optString("category", "Nouveaux"), o.optBoolean("favorite"), o.optLong("updatedAt"))
    }
}.getOrDefault(emptyList())

private fun loadCategories(prefs: android.content.SharedPreferences): List<NoteCategory> = runCatching {
    val raw = prefs.getString("categories", null) ?: return@runCatching starterCategories
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        NoteCategory(o.getString("name"), o.optString("emoji", "📁"), o.optLong("color", 0xFF6C9B80))
    }
}.getOrDefault(starterCategories)

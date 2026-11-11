package com.leafvibe.leafnotes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
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

private data class Note(val id: String = UUID.randomUUID().toString(), val title: String, val body: String, val category: String, val favorite: Boolean = false, val updatedAt: Long = System.currentTimeMillis())
private data class NoteCategory(val name: String, val emoji: String, val color: Long, val parent: String? = null)
private const val FAVORITES_FILTER = "__leafnotes_favorites_filter__"
private val starterCategories = listOf(
    NoteCategory("Nouveaux", "✨", 0xFF8E7CC3), NoteCategory("Idées", "💡", 0xFF4D9B83),
    NoteCategory("Cours", "📚", 0xFF5685C5), NoteCategory("Informatique", "💻", 0xFF597C91),
    NoteCategory("Archives", "🗄️", 0xFF8B8B8B)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { LeafNotesApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeafNotesApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("leafnotes", Context.MODE_PRIVATE) }
    var notes by remember { mutableStateOf(loadNotes(prefs)) }
    var categories by remember { mutableStateOf(loadCategories(prefs)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var selected by remember { mutableStateOf("Tous") }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var editor by remember { mutableStateOf(false) }
    var categoryLibrary by remember { mutableStateOf(false) }
    var parent by remember { mutableStateOf<String?>(null) }
    var categoryPage by remember { mutableIntStateOf(0) }
    var categoryEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }

    fun saveNotes(v: List<Note>) {
        notes = v
        prefs.edit().putString("notes", JSONArray().apply { v.forEach { n -> put(JSONObject().apply {
            put("id", n.id); put("title", n.title); put("body", n.body); put("category", n.category)
            put("favorite", n.favorite); put("updatedAt", n.updatedAt)
        }) } }.toString()).apply()
    }
    fun saveCategories(v: List<NoteCategory>) {
        categories = v
        prefs.edit().putString("categories", JSONArray().apply { v.forEach { c -> put(JSONObject().apply {
            put("name", c.name); put("emoji", c.emoji); put("color", c.color); if (c.parent != null) put("parent", c.parent)
        }) } }.toString()).apply()
    }

    val visible = notes.filter { n ->
        val match = when (selected) { "Tous" -> true; FAVORITES_FILTER -> n.favorite; else -> n.category == selected }
        match && (query.isBlank() || n.title.contains(query, true) || n.body.contains(query, true))
    }.sortedByDescending { it.updatedAt }
    val pages = maxOf(1, (visible.size + 4) / 5)
    val safePage = page.coerceIn(0, pages - 1)
    val pageNotes = visible.drop(safePage * 5).take(5)

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        if (categoryLibrary) {
            val children = categories.filter { it.parent == parent }.sortedBy { it.name.lowercase() }
            val countPages = maxOf(1, (children.size + 4) / 5)
            val safe = categoryPage.coerceIn(0, countPages - 1)
            val shown = children.drop(safe * 5).take(5)
            Scaffold(topBar = {
                TopAppBar(title = { Text(parent ?: "Toutes les catégories") }, navigationIcon = {
                    IconButton(onClick = { parent = categories.firstOrNull { it.name == parent }?.parent; categoryPage = 0 }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }, actions = { IconButton(onClick = { categoryEditor = true }) { Icon(Icons.Default.Add, contentDescription = "Créer") } })
            }, floatingActionButton = { FloatingActionButton(onClick = { categoryEditor = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter une catégorie") } }) { pad ->
                Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                    Text("Dossiers et sous-dossiers • " + children.size + " éléments", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    if (shown.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Aucun dossier ici. Appuie sur + pour en créer un.") }
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.name }) { c ->
                            val childCount = categories.count { it.parent == c.name }
                            val noteCount = notes.count { it.category == c.name }
                            Card(Modifier.fillMaxWidth().clickable {
                                if (childCount > 0) { parent = c.name; categoryPage = 0 }
                                else { selected = c.name; query = ""; page = 0; categoryLibrary = false }
                            }) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.emoji, style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.SemiBold)
                                        Text(if (childCount > 0) childCount.toString() + " sous-dossier(s) • " + noteCount + " note(s)" else noteCount.toString() + " note(s)",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(if (childCount > 0) Icons.Default.Folder else Icons.Default.ArrowForward, contentDescription = "Ouvrir")
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { categoryPage = (safe - 1).coerceAtLeast(0) }, enabled = safe > 0) { Icon(Icons.Default.ArrowBack, null) }
                        Text((safe + 1).toString() + " / " + countPages)
                        IconButton(onClick = { categoryPage = (safe + 1).coerceAtMost(countPages - 1) }, enabled = safe < countPages - 1) { Icon(Icons.Default.ArrowForward, null) }
                    }
                }
            }
        } else {
            Scaffold(floatingActionButton = { FloatingActionButton(onClick = { editing = null; editor = true }) { Icon(Icons.Default.Add, contentDescription = "Nouvelle note") } }) { pad ->
                Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("LeafNotes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Ton espace, tes idées.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { parent = null; categoryPage = 0; categoryLibrary = true }) {
                            Icon(Icons.Default.Folder, null); Text("Catégories")
                        }
                        IconButton(onClick = { dark = !dark; prefs.edit().putBoolean("dark", dark).apply() }) { Text(if (dark) "☀️" else "🌙") }
                    }
                    OutlinedTextField(query, { query = it; page = 0 }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Rechercher dans tes notes…") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected == "Tous", { selected = "Tous"; page = 0 }, label = { Text("Tout") })
                        Spacer(Modifier.width(6.dp))
                        FilterChip(selected == FAVORITES_FILTER, { selected = FAVORITES_FILTER; page = 0 }, label = { Text("⭐ Favoris") })
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { parent = null; categoryLibrary = true }) { Text("Voir les catégories") }
                    }
                    Text(if (selected == "Tous") "Ma bibliothèque" else if (selected == FAVORITES_FILTER) "Favoris" else selected,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(visible.size.toString() + " note(s)", style = MaterialTheme.typography.labelMedium)
                    if (pageNotes.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (query.isBlank()) "Aucune note ici. Appuie sur +." else "Aucun résultat") }
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pageNotes, key = { it.id }) { n ->
                            Card(Modifier.fillMaxWidth().clickable { editing = n; editor = true }) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(n.title.ifBlank { "Sans titre" }, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { saveNotes(notes.map { if (it.id == n.id) it.copy(favorite = !it.favorite) else it }) }) {
                                            Icon(if (n.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (n.favorite) Color(0xFFE45B72) else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { saveNotes(notes.filterNot { it.id == n.id }) }) { Icon(Icons.Default.Delete, null) }
                                    }
                                    Text(n.body, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(n.category + " • " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(n.updatedAt)),
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { page = (safePage - 1).coerceAtLeast(0) }, enabled = safePage > 0) { Icon(Icons.Default.ArrowBack, null) }
                        Text((safePage + 1).toString() + " / " + pages)
                        IconButton(onClick = { page = (safePage + 1).coerceAtMost(pages - 1) }, enabled = safePage < pages - 1) { Icon(Icons.Default.ArrowForward, null) }
                    }
                }
            }
        }
    }

    if (editor) {
        var title by remember(editing) { mutableStateOf(editing?.title.orEmpty()) }
        var body by remember(editing) { mutableStateOf(editing?.body.orEmpty()) }
        var category by remember(editing) { mutableStateOf(editing?.category ?: categories.firstOrNull()?.name ?: "Nouveaux") }
        AlertDialog(onDismissRequest = { editor = false }, title = { Text(if (editing == null) "Nouvelle note" else "Modifier la note") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Titre") })
                OutlinedTextField(body, { body = it }, label = { Text("Contenu") }, minLines = 4)
                Text("Enregistrer dans : " + category)
                categories.forEach { c -> TextButton(onClick = { category = c.name }) { Text((if (c.parent == null) "" else "↳ ") + c.emoji + " " + c.name) } }
            } },
            confirmButton = { TextButton(onClick = {
                val old = editing
                saveNotes(if (old == null) notes + Note(title = title.trim(), body = body, category = category)
                    else notes.map { if (it.id == old.id) old.copy(title = title.trim(), body = body, category = category, updatedAt = System.currentTimeMillis()) else it })
                editor = false
            }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { editor = false }) { Text("Annuler") } })
    }

    if (categoryEditor) AlertDialog(onDismissRequest = { categoryEditor = false }, title = { Text(if (parent == null) "Nouvelle catégorie" else "Nouveau sous-dossier") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
            OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true)
            Text(if (parent == null) "Emplacement : racine" else "Dans : " + parent)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            val clean = name.trim()
            if (!clean.equals("Tous", true) && categories.none { it.name.equals(clean, true) })
                saveCategories(categories + NoteCategory(clean, emoji.ifBlank { "📁" }, 0xFF6C9B80, parent))
            name = ""; emoji = "📁"; categoryEditor = false
        }) { Text("Créer") } },
        dismissButton = { TextButton(onClick = { categoryEditor = false }) { Text("Annuler") } })
}

private fun loadNotes(prefs: android.content.SharedPreferences): List<Note> = runCatching {
    val a = JSONArray(prefs.getString("notes", "[]"))
    (0 until a.length()).map { i -> val o = a.getJSONObject(i); Note(o.optString("id"), o.optString("title"), o.optString("body"), o.optString("category", "Nouveaux"), o.optBoolean("favorite"), o.optLong("updatedAt")) }
}.getOrDefault(emptyList())

private fun loadCategories(prefs: android.content.SharedPreferences): List<NoteCategory> = runCatching {
    val raw = prefs.getString("categories", null) ?: return@runCatching starterCategories
    val a = JSONArray(raw)
    (0 until a.length()).map { i -> val o = a.getJSONObject(i); NoteCategory(o.getString("name"), o.optString("emoji", "📁"), o.optLong("color", 0xFF6C9B80), o.optString("parent").takeIf { it.isNotBlank() }) }
}.getOrDefault(starterCategories)

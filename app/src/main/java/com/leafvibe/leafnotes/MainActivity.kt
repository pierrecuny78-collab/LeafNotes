package com.leafvibe.leafnotes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.platform.LocalConfiguration
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

private val Oasis = Color(0xFF206B4D)
private val Walnut = Color(0xFF6B4935)
private val NaturalGold = Color(0xFFC5A46D)
private val Cream = Color(0xFFDCCBB1)
private val SandBorder = Color(0xFFB69A7B)

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
    var showCategoryManager by remember { mutableStateOf(false) }
    var showGraph by remember { mutableStateOf(false) }
    var showSwipe by remember { mutableStateOf(false) }
    var showLearning by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }

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

    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(buildBackupJson(notes, categories).toByteArray(Charsets.UTF_8)) }
        }
    }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Fichier vide")
            val root = JSONObject(raw)
            val importedNotes = parseBackupNotes(root.optJSONArray("notes") ?: JSONArray())
            val importedCategories = parseBackupCategories(root.optJSONArray("categories") ?: JSONArray())
            if (importedCategories.none { it.name == "Nouveaux" }) saveCategories(listOf(NoteCategory("Nouveaux", "✨", 0xFF8E7CC3)) + importedCategories)
            else saveCategories(importedCategories)
            saveNotes(importedNotes)
        }
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

    val appColors = if (darkTheme) {
        darkColorScheme(primary = Oasis, secondary = Walnut, tertiary = NaturalGold)
    } else {
        lightColorScheme(primary = Oasis, secondary = Walnut, tertiary = NaturalGold,
            background = Cream, surface = Cream, outline = SandBorder)
    }
    MaterialTheme(colorScheme = appColors) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🍃 LeafVibe", color = Oasis, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
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
                    item { AssistChip(onClick = { showCategoryManager = true }, label = { Text("Gérer les dossiers") }) }
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { AssistChip(onClick = { showGraph = true }, label = { Text("LeafGraph") }) }
                    item { AssistChip(onClick = { showSwipe = true }, label = { Text("LeafSwipe") }) }
                    item { AssistChip(onClick = { showLearning = true }, label = { Text("LeafLearning") }) }
                    item { AssistChip(onClick = { showQuiz = true }, label = { Text("LeafQuiz") }) }
                    item { AssistChip(onClick = { showBackup = true }, label = { Text("LeafBackup") }) }
                }
                Spacer(Modifier.height(8.dp))
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
                    val screenWidth = LocalConfiguration.current.screenWidthDp
                    val columns = when {
                        screenWidth >= 1200 -> 5
                        screenWidth >= 840 -> 3
                        screenWidth >= 600 -> 2
                        else -> 1
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)) {
                        gridItems(pageNotes, key = { it.id }) { note ->
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
    if (showCategoryManager) CategoryManagerDialog(categories, { showCategoryManager = false }) { categoryName ->
        if (categoryName != "Nouveaux") {
            saveNotes(notes.map { note -> if (note.category == categoryName) note.copy(category = "Nouveaux") else note })
            saveCategories(categories.filterNot { it.name == categoryName })
            if (selectedCategory == categoryName) selectedCategory = "Tous"
        }
    }
    if (showGraph) LeafGraphDialog(notes, onOpen = { note -> editing = note; showEditor = true; showGraph = false }) { showGraph = false }
    if (showSwipe) LeafSwipeDialog(notes, onOpen = { note -> editing = note; showEditor = true; showSwipe = false }) { showSwipe = false }
    if (showLearning) LeafLearningDialog(notes) { showLearning = false }
    if (showQuiz) LeafQuizDialog(notes) { showQuiz = false }
    if (showBackup) LeafBackupDialog(
        onExport = { backupExport.launch("LeafNotes-sauvegarde.json") },
        onImport = { backupImport.launch(arrayOf("application/json", "text/*")) },
        onDismiss = { showBackup = false }
    )
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
    var confirmDelete by remember(note.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit),
        border = androidx.compose.foundation.BorderStroke(1.dp, SandBorder),
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
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
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
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette note ?") },
            text = { Text("Cette action supprimera définitivement cette note de la bibliothèque locale.") },
            confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
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


@Composable
private fun CategoryManagerDialog(categories: List<NoteCategory>, onDismiss: () -> Unit, onDelete: (String) -> Unit) {
    var pendingDelete by remember { mutableStateOf<NoteCategory?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gérer les catégories et dossiers") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Les notes d'un dossier supprimé seront déplacées dans « Nouveaux ».", style = MaterialTheme.typography.bodySmall)
                categories.forEach { category ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji + " " + category.name, Modifier.weight(1f))
                        if (category.name == "Nouveaux") {
                            Text("Protégé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TextButton(onClick = { pendingDelete = category }) { Text("Supprimer") }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terminé") } }
    )
    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer « " + category.name + " » ?") },
            text = { Text("Les notes de cette catégorie seront conservées et déplacées dans « Nouveaux ».") },
            confirmButton = {
                TextButton(onClick = { onDelete(category.name); pendingDelete = null }) { Text("Supprimer le dossier") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun LeafGraphDialog(notes: List<Note>, onOpen: (Note) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LeafGraph · Carte de tes connaissances") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Les notes sont regroupées par catégorie. Ouvre une note pour la modifier et développer ton réseau de connaissances.",
                    style = MaterialTheme.typography.bodySmall)
                if (notes.isEmpty()) Text("Crée d'abord quelques notes pour construire ton graphe.")
                notes.groupBy { it.category }.forEach { (category, grouped) ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                            Text("📁 " + category + " · " + grouped.size, fontWeight = FontWeight.Bold)
                            grouped.forEach { note ->
                                TextButton(onClick = { onOpen(note) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("●  " + note.title.ifBlank { "Sans titre" }, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun LeafSwipeDialog(notes: List<Note>, onOpen: (Note) -> Unit, onDismiss: () -> Unit) {
    var index by remember(notes) { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, (notes.size - 1).coerceAtLeast(0))
    val note = notes.getOrNull(safeIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LeafSwipe · Révision rapide") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (notes.isEmpty()) "Aucune note à réviser" else (safeIndex + 1).toString() + " / " + notes.size,
                    style = MaterialTheme.typography.labelMedium)
                if (note != null) {
                    Card(
                        Modifier.fillMaxWidth().heightIn(min = 190.dp)
                            .pointerInput(safeIndex, notes.size) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -18) { index = (index + 1).coerceAtMost(notes.lastIndex); revealed = false }
                                    if (dragAmount > 18) { index = (index - 1).coerceAtLeast(0); revealed = false }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(note.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Text(note.title.ifBlank { "Sans titre" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (revealed) Text(note.body.ifBlank { "Cette note ne contient pas encore de réponse." })
                            else Text("Essaie de te rappeler le contenu, puis affiche la réponse.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { revealed = !revealed }) { Text(if (revealed) "Masquer la réponse" else "Afficher la réponse") }
                            TextButton(onClick = { onOpen(note) }) { Text("Modifier cette note") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { index = (index - 1).coerceAtLeast(0); revealed = false }, enabled = safeIndex > 0) { Text("Précédent") }
                        Button(onClick = { index = (index + 1).coerceAtMost(notes.lastIndex); revealed = false }, enabled = safeIndex < notes.lastIndex) { Text("Suivant") }
                    }
                    Text("Tu peux aussi faire glisser la carte horizontalement.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terminé") } }
    )
}

@Composable
private fun LeafLearningDialog(notes: List<Note>, onDismiss: () -> Unit) {
    var index by remember(notes) { mutableIntStateOf(0) }
    var answerVisible by remember { mutableStateOf(false) }
    var mastered by remember { mutableIntStateOf(0) }
    var toReview by remember { mutableIntStateOf(0) }
    val note = notes.getOrNull(index.coerceIn(0, (notes.size - 1).coerceAtLeast(0)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LeafLearning · Apprendre activement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Rappelle-toi d'abord, reformule avec tes mots, puis vérifie. Ce système utilise tes notes comme cartes de révision.",
                    style = MaterialTheme.typography.bodySmall)
                if (note == null) Text("Ajoute des notes pour commencer.")
                else {
                    Text((index + 1).toString() + " / " + notes.size + " · " + note.category, color = MaterialTheme.colorScheme.primary)
                    Text(note.title.ifBlank { "Sans titre" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (answerVisible) Text(note.body.ifBlank { "Ajoute une réponse à cette note pour mieux l'étudier." })
                    else Text("Sans regarder la note, explique le sujet à voix haute ou sur une feuille.")
                    Button(onClick = { answerVisible = !answerVisible }) { Text(if (answerVisible) "Cacher" else "Vérifier ma réponse") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { toReview++; index = (index + 1).coerceAtMost(notes.lastIndex); answerVisible = false }) { Text("À revoir") }
                        Button(onClick = { mastered++; index = (index + 1).coerceAtMost(notes.lastIndex); answerVisible = false }) { Text("Je maîtrise") }
                    }
                    Text("Maîtrisées : " + mastered + " · À revoir : " + toReview, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun LeafQuizDialog(notes: List<Note>, onDismiss: () -> Unit) {
    var index by remember(notes) { mutableIntStateOf(0) }
    var answerVisible by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    val note = notes.getOrNull(index.coerceIn(0, (notes.size - 1).coerceAtLeast(0)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LeafQuiz · Quiz à partir de tes notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (note == null) Text("Ajoute des notes pour générer tes premières questions.")
                else {
                    Text("Question " + (index + 1) + " / " + notes.size, color = MaterialTheme.colorScheme.primary)
                    Text(note.title.ifBlank { "Quel est le sujet de cette note ?" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (answerVisible) Text(note.body.ifBlank { "Aucune réponse enregistrée." })
                    else Text("Essaie de répondre sans consulter ton cours.")
                    Button(onClick = { answerVisible = !answerVisible }) { Text(if (answerVisible) "Masquer" else "Voir la réponse") }
                    if (answerVisible) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { wrong++; index = (index + 1).coerceAtMost(notes.lastIndex); answerVisible = false }) { Text("Incorrect") }
                        Button(onClick = { correct++; index = (index + 1).coerceAtMost(notes.lastIndex); answerVisible = false }) { Text("Correct") }
                    }
                    Text("Réponses correctes : " + correct + " · À retravailler : " + wrong, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Terminé") } }
    )
}

@Composable
private fun LeafBackupDialog(onExport: () -> Unit, onImport: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LeafBackup · Sauvegarde locale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Exporte tes notes et catégories dans un fichier JSON, puis conserve-le sur ta carte SD ou un autre support.")
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sauvegarde") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Importer une sauvegarde") }
                Text("L'import remplace la bibliothèque actuelle. Fais une sauvegarde avant de restaurer.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

private fun buildBackupJson(notes: List<Note>, categories: List<NoteCategory>): String = JSONObject().apply {
    put("format", "leafnotes-backup")
    put("version", 1)
    put("createdAt", System.currentTimeMillis())
    put("notes", JSONArray().apply {
        notes.forEach { n -> put(JSONObject().apply {
            put("id", n.id); put("title", n.title); put("body", n.body)
            put("category", n.category); put("favorite", n.favorite); put("updatedAt", n.updatedAt)
        }) }
    })
    put("categories", JSONArray().apply {
        categories.forEach { c -> put(JSONObject().apply {
            put("name", c.name); put("emoji", c.emoji); put("color", c.color)
        }) }
    })
}.toString(2)

private fun parseBackupNotes(array: JSONArray): List<Note> = (0 until array.length()).map { i ->
    val o = array.getJSONObject(i)
    Note(o.optString("id", UUID.randomUUID().toString()), o.optString("title"), o.optString("body"),
        o.optString("category", "Nouveaux"), o.optBoolean("favorite"), o.optLong("updatedAt", System.currentTimeMillis()))
}

private fun parseBackupCategories(array: JSONArray): List<NoteCategory> = (0 until array.length()).map { i ->
    val o = array.getJSONObject(i)
    NoteCategory(o.optString("name", "Dossier"), o.optString("emoji", "📁"), o.optLong("color", 0xFF6C9B80))
}.distinctBy { it.name.lowercase() }.filter { it.name.isNotBlank() }

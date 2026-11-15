package com.leafvibe.leafnotes

import android.content.Context
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import android.widget.Toast

internal data class Note(val id: String = UUID.randomUUID().toString(), val title: String, val body: String, val category: String, val favorite: Boolean = false, val updatedAt: Long = System.currentTimeMillis(), val tags: List<String> = emptyList(), val checklist: List<ChecklistItem> = emptyList(), val flashcards: List<Flashcard> = emptyList(), val icon: String = "📝", val reminderAt: Long = 0L, val revisions: List<NoteRevision> = emptyList())
internal data class NoteCategory(val name: String, val emoji: String, val color: Long, val parent: String? = null)
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
    val store = remember { LeafNotesStore(prefs) }
    var notes by remember { mutableStateOf(store.loadNotes()) }
    var categories by remember { mutableStateOf(store.loadCategories(starterCategories)) }
    var trash by remember { mutableStateOf(store.loadTrash()) }
    var templates by remember { mutableStateOf(store.loadTemplates()) }
    var toolsDialog by remember { mutableStateOf(false) }
    var trashDialog by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var selected by remember { mutableStateOf("Tous") }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var editor by remember { mutableStateOf(false) }
    var categoryLibrary by remember { mutableStateOf(false) }
    var graphOpen by remember { mutableStateOf(false) }
    var quizOpen by remember { mutableStateOf(false) }
    var swipeOpen by remember { mutableStateOf(false) }
    var learningOpen by remember { mutableStateOf(false) }
    var settingsDialog by remember { mutableStateOf(false) }
    var parent by remember { mutableStateOf<String?>(null) }
    var categoryPage by remember { mutableIntStateOf(0) }
    var categoryEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }
    var categoryColor by remember { mutableStateOf("#6C9B80") }
    var categoryBeingEdited by remember { mutableStateOf<NoteCategory?>(null) }
    var reminderTarget by remember { mutableStateOf<Note?>(null) }
    var permissionPending by remember { mutableStateOf<Note?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = permissionPending
        permissionPending = null
        if (granted && target != null) reminderTarget = target
        else if (!granted) Toast.makeText(context, "Permission de notification refusée.", Toast.LENGTH_LONG).show()
    }

    fun saveNotes(v: List<Note>) {
        store.saveNotes(v)
        notes = v
    }
    fun saveCategories(v: List<NoteCategory>) {
        store.saveCategories(v)
        categories = v
    }
    fun saveTrash(v: List<TrashedNote>) {
        store.saveTrash(v)
        trash = v
    }
    fun saveTemplates(v: List<NoteTemplate>) {
        store.saveTemplates(v)
        templates = v
    }
    fun requestReminder(note: Note) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionPending = note
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else reminderTarget = note
    }
    fun cancelReminder(note: Note) {
        WorkManager.getInstance(context).cancelUniqueWork("leafnotes-reminder-" + note.id)
        saveNotes(notes.map { if (it.id == note.id) it.copy(reminderAt = 0L) else it })
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(LeafNotesTransfer.encode(notes, categories, trash, templates))
                } ?: error("Impossible d'ouvrir le fichier de destination.")
            }
            Toast.makeText(context, if (result.isSuccess) "Sauvegarde exportée." else "Échec de l'export.", Toast.LENGTH_LONG).show()
        }
    }
    val zipExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(LeafNotesTransfer.encodeZip(notes, categories, trash, templates))
                } ?: error("Impossible d'ouvrir le fichier ZIP.")
            }
            Toast.makeText(context, if (result.isSuccess) "Archive ZIP créée." else "Échec de l'export ZIP.", Toast.LENGTH_LONG).show()
        }
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val result = runCatching { LeafNotesPdf.write(context, uri, notes) }
            Toast.makeText(context, if (result.isSuccess) "Export PDF terminé." else "Échec de l'export PDF.", Toast.LENGTH_LONG).show()
        }
    }

    val textExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(LeafNotesTransfer.exportText(notes)) }
                    ?: error("Impossible d'ouvrir le fichier de destination.")
            }
            Toast.makeText(context, if (result.isSuccess) "Export TXT terminé." else "Échec de l'export TXT.", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 50 * 1024 * 1024) { "Sauvegarde supérieure à 50 Mo." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: error("Impossible de lire le fichier sélectionné.")
                val archive = LeafNotesTransfer.decode(bytes)
                val merged = LeafNotesTransfer.merge(notes, categories, archive, trash, templates)
                saveNotes(merged.notes)
                saveCategories(merged.categories)
                saveTrash(merged.trash)
                saveTemplates(merged.templates)
                merged.notes.filter { it.reminderAt > System.currentTimeMillis() }.forEach { scheduleNoteReminderAt(context, it, it.reminderAt) }
                merged
            }
            Toast.makeText(context, result.fold(
                { "Import terminé : " + it.addedNotes + " note(s), " + it.addedCategories + " catégorie(s), " + it.skippedNotes + " doublon(s) ignoré(s)." },
                { "Import impossible : " + (it.message ?: "erreur inconnue") }
            ), Toast.LENGTH_LONG).show()
        }
    }

    val visible = notes.filter { n ->
        val match = when (selected) { "Tous" -> true; FAVORITES_FILTER -> n.favorite; else -> n.category == selected }
        match && (query.isBlank() || LeafNotesOrganization.searchMatches(n, query, n.tags))
    }.sortedByDescending { it.updatedAt }
    val pages = maxOf(1, (visible.size + 4) / 5)
    val safePage = page.coerceIn(0, pages - 1)
    val pageNotes = visible.drop(safePage * 5).take(5)

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        if (swipeOpen) {
            LeafSwipeScreen(notes = notes, onOpenNote = { note -> editing = note; editor = true; swipeOpen = false }, onClose = { swipeOpen = false })
        } else if (learningOpen) {
            LeafLearningScreen(notes = notes, onClose = { learningOpen = false })
        } else if (quizOpen) {
            LeafQuizScreen(notes = notes, onUpdateNote = { updated -> saveNotes(notes.map { if (it.id == updated.id) updated else it }) }, onClose = { quizOpen = false })
        } else if (graphOpen) {
            LeafGraphScreen(
                notes = notes,
                categories = categories,
                onClose = { graphOpen = false },
                onOpenNote = { note -> editing = note; editor = true; graphOpen = false }
            )
        } else if (categoryLibrary) {
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
                    parent?.let { folder ->
                        val directNotes = notes.count { it.category == folder }
                        if (directNotes > 0) {
                            OutlinedButton(onClick = { selected = folder; page = 0; categoryLibrary = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("Voir les notes de ce dossier ($directNotes)")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (shown.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Aucun dossier ici. Appuie sur + pour en créer un.") }
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.name }) { c ->
                            val childCount = categories.count { it.parent == c.name }
                            val noteCount = notes.count { it.category == c.name }
                            Card(Modifier.fillMaxWidth().clickable {
                                if (childCount > 0) { parent = c.name; categoryPage = 0 }
                                else { selected = c.name; query = ""; page = 0; categoryLibrary = false }
                            }, colors = CardDefaults.cardColors(containerColor = Color(c.color.toInt()).copy(alpha = 0.12f))) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.emoji, style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.SemiBold)
                                        Text(if (childCount > 0) childCount.toString() + " sous-dossier(s) • " + noteCount + " note(s)" else noteCount.toString() + " note(s)",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(if (childCount > 0) Icons.Default.Folder else Icons.Default.ArrowForward, contentDescription = "Ouvrir")
                                IconButton(onClick = {
                                    categoryBeingEdited = c
                                    name = c.name
                                    emoji = c.emoji
                                    categoryColor = String.format("#%06X", (c.color and 0xFFFFFF))
                                    categoryEditor = true
                                }) { Icon(Icons.Default.Edit, contentDescription = "Modifier la catégorie") }
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
                        TextButton(onClick = { graphOpen = true }) { Text("🕸️ Graph") }
                        TextButton(onClick = { quizOpen = true }) { Text("Quiz") }
                        TextButton(onClick = { toolsDialog = true }) { Text("LeafBackup") }
                        IconButton(onClick = { dark = !dark; prefs.edit().putBoolean("dark", dark).apply() }) { Text(if (dark) "☀️" else "🌙") }
                        IconButton(onClick = { settingsDialog = true }) { Icon(Icons.Default.Settings, contentDescription = "Paramètres et aide") }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = { swipeOpen = true }, label = { Text("LeafSwipe") })
                        AssistChip(onClick = { learningOpen = true }, label = { Text("LeafLearning") })
                        AssistChip(onClick = { quizOpen = true }, label = { Text("LeafQuiz") })
                    }
                    Text(if (selected == "Tous") "Ma bibliothèque" else if (selected == FAVORITES_FILTER) "Favoris" else selected,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(visible.size.toString() + " note(s)", style = MaterialTheme.typography.labelMedium)
                    if (pageNotes.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (query.isBlank()) "Aucune note ici. Appuie sur +." else "Aucun résultat") }
                    else LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pageNotes, key = { it.id }) { n ->
                            Card(Modifier.fillMaxWidth().clickable { editing = n; editor = true }) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(n.icon, modifier = Modifier.padding(end = 6.dp))
                                        Text(n.title.ifBlank { "Sans titre" }, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { saveNotes(notes.map { if (it.id == n.id) it.copy(favorite = !it.favorite) else it }) }) {
                                            Icon(if (n.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (n.favorite) Color(0xFFE45B72) else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = {
                                            WorkManager.getInstance(context).cancelUniqueWork("leafnotes-reminder-" + n.id)
                                            val moved = LeafNotesTrash.moveToTrash(notes, trash, n.id)
                                            saveNotes(moved.first)
                                            saveTrash(moved.second)
                                        }) { Icon(Icons.Default.Delete, contentDescription = "Mettre à la corbeille") }
                                    }
                                    Text(n.body, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (n.reminderAt > 0L) {
                                        Text("⏰ Rappel : " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(n.reminderAt)), style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = { cancelReminder(n) }) { Text("Annuler le rappel") }
                                    }
                                    Text(n.category + " • " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(n.updatedAt)),
                                        style = MaterialTheme.typography.labelSmall)
                                    TextButton(onClick = { requestReminder(n) }) { Text("Programmer un rappel") }
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
        var tagsText by remember(editing) { mutableStateOf(editing?.tags?.joinToString(", ").orEmpty()) }
        var checklist by remember(editing) { mutableStateOf(editing?.checklist ?: emptyList()) }
        var checklistDraft by remember(editing) { mutableStateOf("") }
        var flashcards by remember(editing) { mutableStateOf(editing?.flashcards ?: emptyList()) }
        var questionDraft by remember(editing) { mutableStateOf("") }
        var answerDraft by remember(editing) { mutableStateOf("") }
        var noteIcon by remember(editing) { mutableStateOf(editing?.icon ?: "📝") }
        var showHistory by remember(editing) { mutableStateOf(false) }
        var showTemplatePicker by remember(editing) { mutableStateOf(false) }
        var category by remember(editing) { mutableStateOf(editing?.category ?: categories.firstOrNull()?.name ?: "Nouveaux") }
        AlertDialog(onDismissRequest = { editor = false }, title = { Text(if (editing == null) "Nouvelle note" else "Modifier la note") },
            text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(noteIcon, { noteIcon = it }, label = { Text("Icône / emoji de la note") }, singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Titre") })
                OutlinedTextField(body, { body = it }, label = { Text("Contenu") }, minLines = 4)
                OutlinedTextField(tagsText, { tagsText = it }, label = { Text("Étiquettes (séparées par des virgules)") }, singleLine = true)
                Text("Liste à cocher", style = MaterialTheme.typography.titleSmall)
                checklist.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.checked, onCheckedChange = { checked ->
                            checklist = checklist.map { if (it.id == item.id) it.copy(checked = checked) else it }
                        })
                        Text(item.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = { checklist = checklist.filterNot { it.id == item.id } }) { Icon(Icons.Default.Delete, contentDescription = "Retirer") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(checklistDraft, { checklistDraft = it }, modifier = Modifier.weight(1f), label = { Text("Nouvel élément") }, singleLine = true)
                    IconButton(onClick = {
                        if (checklistDraft.isNotBlank()) {
                            checklist = checklist + ChecklistItem(text = checklistDraft.trim(), position = checklist.size)
                            checklistDraft = ""
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Ajouter à la liste") }
                }
                Text("Questions / réponses pour LeafQuiz", style = MaterialTheme.typography.titleSmall)
                flashcards.forEach { card ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Q : " + card.question, fontWeight = FontWeight.SemiBold)
                            Text("R : " + card.answer)
                            TextButton(onClick = { flashcards = flashcards.filterNot { it.id == card.id } }) { Text("Retirer") }
                        }
                    }
                }
                OutlinedTextField(questionDraft, { questionDraft = it }, label = { Text("Nouvelle question") }, singleLine = true)
                OutlinedTextField(answerDraft, { answerDraft = it }, label = { Text("Réponse") }, minLines = 2)
                TextButton(enabled = questionDraft.isNotBlank() && answerDraft.isNotBlank(), onClick = {
                    flashcards = flashcards + Flashcard(question = questionDraft.trim(), answer = answerDraft.trim())
                    questionDraft = ""; answerDraft = ""
                }) { Text("Ajouter à LeafQuiz") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTemplatePicker = true }, modifier = Modifier.weight(1f)) { Text("Utiliser un modèle") }
                    OutlinedButton(onClick = {
                        val templateName = title.trim().ifBlank { "Modèle " + (templates.size + 1) }
                        saveTemplates(templates + NoteTemplate(name = templateName, title = title, body = body, category = category))
                        Toast.makeText(context, "Modèle enregistré.", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f)) { Text("Enregistrer comme modèle") }
                }
                if (editing != null) OutlinedButton(onClick = { showHistory = true }, modifier = Modifier.fillMaxWidth()) { Text("Historique des versions (" + (editing?.revisions?.size ?: 0) + ")") }
                Text("Enregistrer dans : " + category)
                categories.forEach { c -> TextButton(onClick = { category = c.name }) { Text((if (c.parent == null) "" else "↳ ") + c.emoji + " " + c.name) } }
            } },
            confirmButton = { TextButton(onClick = {
                val old = editing
                val tags = LeafNotesOrganization.normalizeTags(tagsText.split(","))
                saveNotes(if (old == null) notes + Note(title = title.trim(), body = body, category = category, tags = tags, checklist = checklist, flashcards = flashcards, icon = noteIcon.ifBlank { "📝" })
                    else notes.map {
                    if (it.id == old.id) {
                        val changed = old.title != title.trim() || old.body != body
                        val revisions = if (changed) (listOf(NoteRevision(noteId = old.id, title = old.title, body = old.body)) + old.revisions).take(30) else old.revisions
                        old.copy(title = title.trim(), body = body, category = category, tags = tags, checklist = checklist, flashcards = flashcards, icon = noteIcon.ifBlank { "📝" }, updatedAt = System.currentTimeMillis(), revisions = revisions)
                    } else it
                })
                editor = false
            }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { editor = false }) { Text("Annuler") } })

        if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Modèles de notes") },
            text = {
                if (templates.isEmpty()) Text("Aucun modèle enregistré. Prépare une note puis choisis « Enregistrer comme modèle ».")
                else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(templates, key = { it.id }) { template ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(template.name, fontWeight = FontWeight.SemiBold)
                            Text(template.title.ifBlank { "Sans titre" })
                            Text(template.body.take(100), maxLines = 2)
                            TextButton(onClick = {
                                title = template.title
                                body = template.body
                                if (template.category != null && categories.any { it.name == template.category }) category = template.category
                                showTemplatePicker = false
                            }) { Text("Appliquer ce modèle") }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplatePicker = false }) { Text("Fermer") } }
        )
    }

        if (showHistory) {
        val history = editing?.revisions.orEmpty()
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Historique des versions") },
            text = {
                if (history.isEmpty()) Text("Aucune ancienne version. Les modifications du titre et du contenu seront conservées ici.")
                else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(history, key = { it.id }) { revision ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(revision.savedAt)), style = MaterialTheme.typography.labelSmall)
                            Text(revision.title.ifBlank { "Sans titre" }, fontWeight = FontWeight.SemiBold)
                            Text(revision.body.take(120), maxLines = 3)
                            TextButton(onClick = { title = revision.title; body = revision.body; showHistory = false }) { Text("Restaurer dans l'éditeur") }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Fermer") } }
        )
        }
    }

    if (reminderTarget != null) {
        val target = reminderTarget!!
        fun applyReminder(triggerAt: Long) {
            scheduleNoteReminderAt(context, target, triggerAt)
            saveNotes(notes.map { if (it.id == target.id) it.copy(reminderAt = triggerAt) else it })
            reminderTarget = null
        }
        AlertDialog(
            onDismissRequest = { reminderTarget = null },
            title = { Text("Rappel pour " + target.title.ifBlank { "cette note" }) },
            text = { Text("Choisis quand recevoir une notification locale.") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        applyReminder(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10L))
                    }) { Text("Dans 10 minutes") }
                    TextButton(onClick = {
                        applyReminder(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(60L))
                    }) { Text("Dans 1 heure") }
                    TextButton(onClick = {
                        applyReminder(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1440L))
                    }) { Text("Demain") }
                    TextButton(onClick = {
                        val now = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, day ->
                            val chosen = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            TimePickerDialog(context, { _, hour, minute ->
                                chosen.set(Calendar.HOUR_OF_DAY, hour)
                                chosen.set(Calendar.MINUTE, minute)
                                chosen.set(Calendar.SECOND, 0)
                                chosen.set(Calendar.MILLISECOND, 0)
                                if (chosen.timeInMillis <= System.currentTimeMillis()) {
                                    Toast.makeText(context, "Choisis une date future.", Toast.LENGTH_SHORT).show()
                                } else applyReminder(chosen.timeInMillis)
                            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Choisir date et heure…") }
                }
            },
            dismissButton = { TextButton(onClick = { reminderTarget = null }) { Text("Annuler") } }
        )
    }

    if (settingsDialog) AlertDialog(
        onDismissRequest = { settingsDialog = false },
        title = { Text("Paramètres et aide LeafNotes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LeafNotes fonctionne localement et hors ligne. Les notes, dossiers, listes, cartes et rappels sont conservés sur cet appareil.")
                Text("LeafGraph : carte interactive, filtre par catégorie, zoom, formes et fonds personnalisables, liens [[Titre]].")
                Text("LeafSwipe : révision rapide par cartes. LeafLearning : reformulation active. LeafQuiz : questions/réponses avec répétition espacée.")
                Text("LeafBackup : export/import de la bibliothèque et de la corbeille sans écraser les notes locales existantes.")
                Text("IA hybride et synchronisation automatique entre appareils restent des évolutions planifiées, non activées dans cette APK.")
                OutlinedButton(onClick = { settingsDialog = false; toolsDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Ouvrir LeafBackup") }
            }
        },
        confirmButton = { TextButton(onClick = { settingsDialog = false }) { Text("Fermer") } }
    )

    if (toolsDialog) AlertDialog(
        onDismissRequest = { toolsDialog = false },
        title = { Text("Outils et sauvegardes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tes données restent stockées localement sur cet appareil.")
                Button(onClick = {
                    toolsDialog = false
                    exportLauncher.launch("LeafNotes-" + System.currentTimeMillis() + ".leafnotes")
                }, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sauvegarde") }
                OutlinedButton(onClick = {
                    toolsDialog = false
                    zipExportLauncher.launch("LeafNotes-" + System.currentTimeMillis() + ".zip")
                }, modifier = Modifier.fillMaxWidth()) { Text("Exporter une archive ZIP") }
                OutlinedButton(onClick = {
                    toolsDialog = false
                    pdfExportLauncher.launch("LeafNotes-" + System.currentTimeMillis() + ".pdf")
                }, modifier = Modifier.fillMaxWidth()) { Text("Exporter en PDF") }
                OutlinedButton(onClick = {
                    toolsDialog = false
                    textExportLauncher.launch("LeafNotes-" + System.currentTimeMillis() + ".txt")
                }, modifier = Modifier.fillMaxWidth()) { Text("Exporter les notes en TXT") }
                OutlinedButton(onClick = {
                    toolsDialog = false
                    importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Importer une sauvegarde") }
                OutlinedButton(onClick = {
                    toolsDialog = false
                    trashDialog = true
                }, modifier = Modifier.fillMaxWidth()) { Text("Corbeille (" + trash.size + ")") }
            }
        },
        confirmButton = { TextButton(onClick = { toolsDialog = false }) { Text("Fermer") } }
    )

    if (trashDialog) AlertDialog(
        onDismissRequest = { trashDialog = false },
        title = { Text("Corbeille") },
        text = {
            if (trash.isEmpty()) Text("La corbeille est vide.")
            else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(trash, key = { it.note.id }) { entry ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(entry.note.title.ifBlank { "Sans titre" }, fontWeight = FontWeight.SemiBold)
                            Text(entry.note.category, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    val restored = LeafNotesTrash.restore(trash, entry.note.id, notes)
                                    saveNotes(restored.first)
                                    saveTrash(restored.second)
                                    restored.first.firstOrNull { it.id == entry.note.id }?.takeIf { it.reminderAt > System.currentTimeMillis() }?.let { scheduleNoteReminderAt(context, it, it.reminderAt) }
                                }) { Text("Restaurer") }
                                TextButton(onClick = {
                                    saveTrash(LeafNotesTrash.permanentlyDelete(trash, entry.note.id))
                                }) { Text("Supprimer définitivement") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { trashDialog = false }) { Text("Fermer") } },
        dismissButton = {
            if (trash.isNotEmpty()) TextButton(onClick = { saveTrash(emptyList()) }) { Text("Vider la corbeille") }
        }
    )

    if (categoryEditor) AlertDialog(onDismissRequest = { categoryBeingEdited = null; name = ""; emoji = "📁"; categoryColor = "#6C9B80"; categoryEditor = false }, title = { Text(if (categoryBeingEdited != null) "Modifier la catégorie" else if (parent == null) "Nouvelle catégorie" else "Nouveau sous-dossier") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
            OutlinedTextField(emoji, { emoji = it }, label = { Text("Emoji") }, singleLine = true)
            OutlinedTextField(categoryColor, { categoryColor = it }, label = { Text("Couleur HEX (#RRGGBB)") }, singleLine = true)
            Text(if (categoryBeingEdited != null) "Modification du dossier" else if (parent == null) "Emplacement : racine" else "Dans : " + parent)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            val clean = name.trim()
            val parsedColor = runCatching { android.graphics.Color.parseColor(categoryColor.trim()) }.getOrDefault(android.graphics.Color.rgb(108, 155, 128)).toLong() and 0xFFFFFFFFL
            val oldCategory = categoryBeingEdited
            if (!clean.equals("Tous", true)) {
                if (oldCategory == null && categories.none { it.name.equals(clean, true) }) {
                    saveCategories(categories + NoteCategory(clean, emoji.ifBlank { "📁" }, parsedColor, parent))
                } else if (oldCategory != null && (clean.equals(oldCategory.name, true) || categories.none { it.name.equals(clean, true) })) {
                    val renamed = categories.map { c ->
                        when {
                            c.name == oldCategory.name -> c.copy(name = clean, emoji = emoji.ifBlank { "📁" }, color = parsedColor)
                            c.parent == oldCategory.name -> c.copy(parent = clean)
                            else -> c
                        }
                    }
                    saveCategories(renamed)
                    saveNotes(notes.map { if (it.category == oldCategory.name) it.copy(category = clean) else it })
                    if (parent == oldCategory.name) parent = clean
                }
            }
            name = ""; emoji = "📁"; categoryColor = "#6C9B80"; categoryBeingEdited = null; categoryEditor = false
        }) { Text(if (categoryBeingEdited == null) "Créer" else "Enregistrer") } },
        dismissButton = { TextButton(onClick = { categoryBeingEdited = null; name = ""; emoji = "📁"; categoryColor = "#6C9B80"; categoryEditor = false }) { Text("Annuler") } })
}


private fun scheduleNoteReminderAt(context: Context, note: Note, triggerAt: Long) {
    val input = Data.Builder()
        .putString("note_title", note.title.ifBlank { "Rappel LeafNotes" })
        .putString("note_body", note.body.take(240))
        .putString("note_id", note.id)
        .build()
    val request = OneTimeWorkRequestBuilder<LeafNoteReminderWorker>()
        .setInputData(input)
        .setInitialDelay((triggerAt - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        .addTag("leafnotes-reminder-" + note.id)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "leafnotes-reminder-" + note.id,
        ExistingWorkPolicy.REPLACE,
        request
    )
}

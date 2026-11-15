package com.leafvibe.leafnotes

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.ceil
import kotlin.math.sqrt

private data class LeafGraphPoint(val note: Note, val x: Float, val y: Float)

@Composable
fun LeafGraphCanvasDialog(notes: List<Note>, onOpen: (Note) -> Unit, onDismiss: () -> Unit) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<Note?>(null) }
    var activeCategory by remember { mutableStateOf("Toutes") }
    var backgroundMode by remember { mutableStateOf("Crème") }
    var nodeMode by remember { mutableStateOf("Rond") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var layoutMode by remember { mutableStateOf("Groupé") }
    var colorMode by remember { mutableStateOf("Catégorie") }
    var overlayMode by remember { mutableStateOf(false) }
    var dragMode by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val graphPrefs = remember { context.getSharedPreferences("leafnotes_graph", android.content.Context.MODE_PRIVATE) }
    var savedPositions by remember {
        mutableStateOf(runCatching {
            val root = JSONObject(graphPrefs.getString("positions", "{}") ?: "{}")
            root.keys().asSequence().associateWith { key ->
                val item = root.getJSONObject(key)
                Offset(item.optDouble("x", 0.5).toFloat(), item.optDouble("y", 0.5).toFloat())
            }
        }.getOrDefault(emptyMap()))
    }
    fun persistPositions(values: Map<String, Offset>) {
        val root = JSONObject()
        values.forEach { (id, position) ->
            root.put(id, JSONObject().put("x", position.x.toDouble()).put("y", position.y.toDouble()))
        }
        graphPrefs.edit().putString("positions", root.toString()).apply()
    }
    val categories = remember(notes) { notes.map { it.category }.distinct().sorted() }
    val filteredNotes = remember(notes, activeCategory) {
        if (activeCategory == "Toutes") notes else notes.filter { it.category == activeCategory }
    }
    val points = remember(filteredNotes, layoutMode, savedPositions) {
        val result = mutableListOf<LeafGraphPoint>()
        when (layoutMode) {
            "Grille" -> {
                val columns = ceil(sqrt(filteredNotes.size.coerceAtLeast(1).toDouble())).toInt().coerceAtLeast(1)
                val rows = ceil(filteredNotes.size.coerceAtLeast(1).toDouble() / columns).toInt()
                filteredNotes.forEachIndexed { index, note ->
                    result += LeafGraphPoint(note,
                        0.10f + (index % columns + 0.5f) / columns * 0.80f,
                        0.10f + (index / columns + 0.5f) / rows * 0.80f)
                }
            }
            "Superposé" -> filteredNotes.forEachIndexed { index, note ->
                val layer = index % 7
                result += LeafGraphPoint(note, 0.5f + layer * 0.018f, 0.5f + layer * 0.018f)
            }
            else -> {
                val groups = filteredNotes.groupBy { it.category }.toSortedMap()
                val groupCount = groups.size.coerceAtLeast(1)
                groups.entries.forEachIndexed { groupIndex, entry ->
                    val groupAngle = (2.0 * PI * groupIndex / groupCount).toFloat() - (PI / 2.0).toFloat()
                    val centerX = 0.5f + cos(groupAngle) * if (groupCount == 1) 0f else 0.29f
                    val centerY = 0.5f + sin(groupAngle) * if (groupCount == 1) 0f else 0.29f
                    entry.value.forEachIndexed { noteIndex, note ->
                        val angle = (2.0 * PI * noteIndex / entry.value.size.coerceAtLeast(1)).toFloat()
                        val radius = if (entry.value.size <= 1) 0f else 0.105f + (noteIndex % 3) * 0.018f
                        result += LeafGraphPoint(note, centerX + cos(angle) * radius, centerY + sin(angle) * radius)
                    }
                }
            }
        }
        if (layoutMode == "Libre") result.map { point ->
            val saved = savedPositions[point.note.id]
            if (saved == null) point else LeafGraphPoint(point.note, saved.x, saved.y)
        } else result
    }
    val byTitle = remember(points) { points.associateBy { it.note.title.trim().lowercase() } }
    val links = remember(points) {
        val explicit = mutableSetOf<Pair<String, String>>()
        points.forEach { point ->
            Regex("""\[\[([^\]]+)\]\]""").findAll(point.note.body).forEach { match ->
                val target = byTitle[match.groupValues[1].trim().lowercase()]
                if (target != null && target.note.id != point.note.id) {
                    explicit += point.note.id to target.note.id
                }
            }
        }
        explicit.toList()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("LeafGraph", style = MaterialTheme.typography.headlineSmall)
                        Text("Carte interactive · ${filteredNotes.size} notes · ${links.size} liens explicites",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        zoom = 1f
                        pan = Offset.Zero
                        savedPositions = emptyMap()
                        persistPositions(savedPositions)
                        layoutMode = "Groupé"
                        dragMode = false
                    }) { Text("Réinitialiser") }
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { androidx.compose.material3.FilterChip(selected = activeCategory == "Toutes", onClick = { activeCategory = "Toutes" }, label = { Text("Toutes") }) }
                    items(categories) { category ->
                        androidx.compose.material3.FilterChip(selected = activeCategory == category, onClick = { activeCategory = category }, label = { Text(category) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    items(listOf("Groupé", "Grille", "Superposé", "Libre")) { mode ->
                        androidx.compose.material3.FilterChip(selected = layoutMode == mode,
                            onClick = { layoutMode = mode; dragMode = mode == "Libre" }, label = { Text(mode) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Crème", "Nuit", "Sauge", "Papier")) { mode ->
                        androidx.compose.material3.FilterChip(selected = backgroundMode == mode, onClick = { backgroundMode = mode }, label = { Text(mode) })
                    }
                    items(listOf("Rond", "Carré", "Losange", "Triangle", "Hexagone")) { mode ->
                        androidx.compose.material3.FilterChip(selected = nodeMode == mode, onClick = { nodeMode = mode }, label = { Text(mode) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 5.dp)) {
                    items(listOf("Catégorie", "Vert", "Bleu", "Chaud")) { mode ->
                        androidx.compose.material3.FilterChip(selected = colorMode == mode, onClick = { colorMode = mode }, label = { Text(mode) })
                    }
                    item {
                        androidx.compose.material3.FilterChip(selected = overlayMode, onClick = { overlayMode = !overlayMode },
                            label = { Text(if (overlayMode) "Liaisons masquées" else "Superposition") })
                    }
                    item {
                        androidx.compose.material3.FilterChip(selected = dragMode, onClick = {
                            dragMode = !dragMode
                            if (dragMode) layoutMode = "Libre"
                        }, label = { Text(if (dragMode) "Déplacement actif" else "Déplacer les nœuds") })
                    }
                }
                Text(if (dragMode) "Déplace les nœuds au doigt ; les positions libres sont conservées sur cet appareil."
                    else "Pince pour zoomer · fais glisser pour déplacer la carte · touche un nœud pour ouvrir la note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
                val graphBackground = when (backgroundMode) {
                    "Nuit" -> Color(0xFF17231E)
                    "Sauge" -> Color(0xFFDCE7DC)
                    "Papier" -> Color(0xFFF1E8D8)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(Modifier.weight(1f).fillMaxWidth().background(graphBackground)) {
                    Canvas(
                        Modifier.fillMaxSize()
                            .pointerInput(points, zoom, pan, dragMode) {
                                if (dragMode) {
                                    detectDragGestures(
                                        onDragStart = { touch ->
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            val world = (touch - center - pan) / zoom + center
                                            val hit = points.minByOrNull { point ->
                                                val dx = point.x * size.width - world.x
                                                val dy = point.y * size.height - world.y
                                                dx * dx + dy * dy
                                            }
                                            draggingId = hit?.takeIf { point ->
                                                val dx = point.x * size.width - world.x
                                                val dy = point.y * size.height - world.y
                                                dx * dx + dy * dy < 58f * 58f
                                            }?.note?.id
                                        },
                                        onDragEnd = { draggingId = null },
                                        onDragCancel = { draggingId = null },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val id = draggingId
                                            if (id != null) {
                                                val center = Offset(size.width / 2f, size.height / 2f)
                                                val world = (change.position - center - pan) / zoom + center
                                                val position = Offset((world.x / size.width).coerceIn(0.03f, 0.97f),
                                                    (world.y / size.height).coerceIn(0.03f, 0.97f))
                                                val updated = savedPositions + (id to position)
                                                savedPositions = updated
                                                persistPositions(updated)
                                            }
                                        }
                                    )
                                } else {
                                    detectTransformGestures { _, panChange, zoomChange, _ ->
                                        zoom = (zoom * zoomChange).coerceIn(0.45f, 4.0f)
                                        pan += panChange
                                    }
                                }
                            }
                            .pointerInput(points, zoom, pan, dragMode) {
                                if (!dragMode) detectTapGestures { tap ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val world = (tap - center - pan) / zoom + center
                                    val hit = points.minByOrNull { point ->
                                        val dx = point.x * size.width - world.x
                                        val dy = point.y * size.height - world.y
                                        dx * dx + dy * dy
                                    }
                                    if (hit != null) {
                                        val dx = hit.x * size.width - world.x
                                        val dy = hit.y * size.height - world.y
                                        if (dx * dx + dy * dy < 42f * 42f) selected = hit.note
                                    }
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        fun mapped(x: Float, y: Float): Offset {
                            val base = Offset(x * size.width, y * size.height)
                            return center + (base - center) * zoom + pan
                        }
                        val native = drawContext.canvas.nativeCanvas
                        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(105, 32, 107, 77)
                            strokeWidth = 2.2f
                        }
                        val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(197, 164, 109)
                            strokeWidth = 4f
                        }
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(35, 45, 39)
                            textSize = 30f
                            textAlign = Paint.Align.CENTER
                        }
                        // Light category clusters: neighbouring notes in a category are visually grouped.
                        if (!overlayMode) points.groupBy { it.note.category }.values.forEach { group ->
                            group.zipWithNext().forEach { (a, b) ->
                                val p1 = mapped(a.x, a.y)
                                val p2 = mapped(b.x, b.y)
                                native.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
                            }
                        }
                        links.forEach { (from, to) ->
                            val a = points.firstOrNull { it.note.id == from }
                            val b = points.firstOrNull { it.note.id == to }
                            if (a != null && b != null) {
                                val p1 = mapped(a.x, a.y)
                                val p2 = mapped(b.x, b.y)
                                native.drawLine(p1.x, p1.y, p2.x, p2.y, linkPaint)
                                // Petite pointe directionnelle pour rendre les relations explicites lisibles.
                                val angle = kotlin.math.atan2(p2.y - p1.y, p2.x - p1.x)
                                val head = 16f
                                val left = Offset(p2.x - head * kotlin.math.cos(angle - 0.48f), p2.y - head * kotlin.math.sin(angle - 0.48f))
                                val right = Offset(p2.x - head * kotlin.math.cos(angle + 0.48f), p2.y - head * kotlin.math.sin(angle + 0.48f))
                                native.drawLine(p2.x, p2.y, left.x, left.y, linkPaint)
                                native.drawLine(p2.x, p2.y, right.x, right.y, linkPaint)
                            }
                        }
                        points.forEach { point ->
                            val p = mapped(point.x, point.y)
                            val radius = (17f * zoom).coerceIn(7f, 44f)
                            val categoryPalette = intArrayOf(
                                android.graphics.Color.rgb(32, 107, 77),
                                android.graphics.Color.rgb(77, 124, 145),
                                android.graphics.Color.rgb(126, 105, 170),
                                android.graphics.Color.rgb(190, 135, 72),
                                android.graphics.Color.rgb(155, 95, 105)
                            )
                            val chosenColor = when (colorMode) {
                                "Vert" -> android.graphics.Color.rgb(32, 107, 77)
                                "Bleu" -> android.graphics.Color.rgb(55, 105, 180)
                                "Chaud" -> android.graphics.Color.rgb(197, 125, 65)
                                else -> if (point.note.favorite) android.graphics.Color.rgb(197, 164, 109)
                                    else categoryPalette[point.note.category.hashCode().ushr(1) % categoryPalette.size]
                            }
                            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chosenColor }
                            when (nodeMode) {
                                "Carré" -> native.drawRoundRect(p.x - radius, p.y - radius, p.x + radius, p.y + radius, 7f, 7f, nodePaint)
                                "Losange" -> {
                                    val path = android.graphics.Path().apply {
                                        moveTo(p.x, p.y - radius); lineTo(p.x + radius, p.y)
                                        lineTo(p.x, p.y + radius); lineTo(p.x - radius, p.y); close()
                                    }
                                    native.drawPath(path, nodePaint)
                                }
                                "Triangle" -> {
                                    val path = android.graphics.Path().apply {
                                        moveTo(p.x, p.y - radius); lineTo(p.x + radius, p.y + radius)
                                        lineTo(p.x - radius, p.y + radius); close()
                                    }
                                    native.drawPath(path, nodePaint)
                                }
                                "Hexagone" -> {
                                    val path = android.graphics.Path()
                                    repeat(6) { side ->
                                        val angle = (PI / 3.0 * side - PI / 2.0).toFloat()
                                        val x = p.x + kotlin.math.cos(angle) * radius
                                        val y = p.y + kotlin.math.sin(angle) * radius
                                        if (side == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    path.close()
                                    native.drawPath(path, nodePaint)
                                }
                                else -> native.drawCircle(p.x, p.y, radius, nodePaint)
                            }
                            textPaint.textSize = (25f * zoom).coerceIn(12f, 42f)
                            val label = point.note.title.ifBlank { "Sans titre" }.take(24)
                            native.drawText(label, p.x, p.y + radius + 24f, textPaint)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.45f) }, modifier = Modifier.weight(1f)) { Text("− Zoom") }
                    OutlinedButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(4f) }, modifier = Modifier.weight(1f)) { Text("+ Zoom") }
                }
                Text("Filtre une catégorie pour isoler son graphe. Les flèches représentent les liens [[Titre exact]] écrits dans les notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    selected?.let { note ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(note.title.ifBlank { "Sans titre" }) },
            text = { Text("${note.category}\n\n${note.body.take(220)}") },
            confirmButton = { Button(onClick = { selected = null; onOpen(note) }) { Text("Ouvrir la note") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Fermer") } }
        )
    }
}

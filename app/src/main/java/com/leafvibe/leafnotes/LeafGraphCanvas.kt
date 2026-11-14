package com.leafvibe.leafnotes

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    val categories = remember(notes) { notes.map { it.category }.distinct().sorted() }
    val filteredNotes = remember(notes, activeCategory) {
        if (activeCategory == "Toutes") notes else notes.filter { it.category == activeCategory }
    }
    val points = remember(filteredNotes) {
        val groups = filteredNotes.groupBy { it.category }.toSortedMap()
        val result = mutableListOf<LeafGraphPoint>()
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
        result
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
                    TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Recentrer") }
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { androidx.compose.material3.FilterChip(selected = activeCategory == "Toutes", onClick = { activeCategory = "Toutes" }, label = { Text("Toutes") }) }
                    items(categories) { category ->
                        androidx.compose.material3.FilterChip(selected = activeCategory == category, onClick = { activeCategory = category }, label = { Text(category) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    items(listOf("Crème", "Nuit", "Sauge", "Papier")) { mode ->
                        androidx.compose.material3.FilterChip(selected = backgroundMode == mode, onClick = { backgroundMode = mode }, label = { Text(mode) })
                    }
                    items(listOf("Rond", "Carré", "Losange")) { mode ->
                        androidx.compose.material3.FilterChip(selected = nodeMode == mode, onClick = { nodeMode = mode }, label = { Text(mode) })
                    }
                }
                Text("Pince pour zoomer · fais glisser pour déplacer · touche un nœud pour ouvrir la note",
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
                            .pointerInput(zoom) {
                                detectTransformGestures { _, panChange, zoomChange, _ ->
                                    zoom = (zoom * zoomChange).coerceIn(0.45f, 4.0f)
                                    pan += panChange
                                }
                            }
                            .pointerInput(points, zoom, pan) {
                                detectTapGestures { tap ->
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
                        points.groupBy { it.note.category }.values.forEach { group ->
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
                            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = if (point.note.favorite) android.graphics.Color.rgb(197, 164, 109)
                                    else android.graphics.Color.rgb(32, 107, 77)
                            }
                            when (nodeMode) {
                                "Carré" -> native.drawRoundRect(p.x - radius, p.y - radius, p.x + radius, p.y + radius, 7f, 7f, nodePaint)
                                "Losange" -> {
                                    val path = android.graphics.Path().apply {
                                        moveTo(p.x, p.y - radius); lineTo(p.x + radius, p.y)
                                        lineTo(p.x, p.y + radius); lineTo(p.x - radius, p.y); close()
                                    }
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

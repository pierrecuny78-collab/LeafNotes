package com.leafvibe.leafnotes

import androidx.compose.runtime.Composable

@Composable
internal fun LeafGraphScreen(
    notes: List<Note>,
    categories: List<NoteCategory>,
    onClose: () -> Unit,
    onOpenNote: (Note) -> Unit
) {
    LeafGraphCanvasDialog(
        notes = notes,
        categories = categories,
        onOpen = onOpenNote,
        onDismiss = onClose
    )
}

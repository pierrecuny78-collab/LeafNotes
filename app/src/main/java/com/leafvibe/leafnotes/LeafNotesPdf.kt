package com.leafvibe.leafnotes

import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

internal object LeafNotesPdf {
    fun write(context: Context, uri: Uri, notes: List<Note>) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(38, 45, 41)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(32, 107, 77)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(105, 115, 108)
            textSize = 9f
        }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 56f

        fun nextPageIfNeeded() {
            if (y > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 56f
            }
        }
        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f, extraGap: Float = 0f) {
            val maxWidth = pageWidth - margin * 2 - indent
            val words = text.replace("\n", " \n ").split(Regex("\\s+"))
            var line = StringBuilder()
            for (word in words) {
                if (word == "\n") {
                    nextPageIfNeeded()
                    canvas.drawText(line.toString(), margin + indent, y, paint)
                    y += paint.textSize * 1.45f
                    line = StringBuilder()
                } else {
                    val candidate = if (line.isEmpty()) word else line.toString() + " " + word
                    if (line.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                        nextPageIfNeeded()
                        canvas.drawText(line.toString(), margin + indent, y, paint)
                        y += paint.textSize * 1.45f
                        line = StringBuilder(word)
                    } else {
                        if (line.isNotEmpty()) line.append(' ')
                        line.append(word)
                    }
                }
            }
            if (line.isNotEmpty()) {
                nextPageIfNeeded()
                canvas.drawText(line.toString(), margin + indent, y, paint)
                y += paint.textSize * 1.45f
            }
            y += extraGap
        }

        drawWrapped("LeafNotes — Export de la bibliothèque", titlePaint, extraGap = 8f)
        drawWrapped("Exporté le " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date()), metaPaint, extraGap = 18f)
        notes.forEachIndexed { index, note ->
            drawWrapped(note.icon + " " + note.title.ifBlank { "Sans titre" }, titlePaint, extraGap = 3f)
            drawWrapped("Catégorie : " + note.category + " • " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(note.updatedAt)), metaPaint, extraGap = 7f)
            drawWrapped(note.body, bodyPaint, extraGap = 5f)
            if (note.tags.isNotEmpty()) drawWrapped("Étiquettes : " + note.tags.joinToString(", "), metaPaint, extraGap = 4f)
            note.checklist.forEach { drawWrapped((if (it.checked) "☑ " else "☐ ") + it.text, bodyPaint, indent = 12f) }
            note.flashcards.forEach {
                drawWrapped("Q : " + it.question, bodyPaint, indent = 12f)
                drawWrapped("R : " + it.answer, bodyPaint, indent = 20f)
            }
            if (index < notes.lastIndex) y += 16f
            nextPageIfNeeded()
        }
        document.finishPage(page)
        try {
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
                ?: error("Impossible d'ouvrir le fichier PDF.")
        } finally {
            document.close()
        }
    }
}

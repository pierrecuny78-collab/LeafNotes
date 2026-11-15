package com.leafvibe.leafnotes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray

class LeafNotesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("leafnotes", Context.MODE_PRIVATE)
        val notes = runCatching { JSONArray(prefs.getString("notes", "[]")) }.getOrDefault(JSONArray())
        val count = notes.length()
        val latestTitle = if (count > 0) notes.optJSONObject(count - 1)?.optString("title").orEmpty().ifBlank { "Dernière note" } else "Aucune note"
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.leafnotes_widget)
            views.setTextViewText(R.id.widget_count, "$count note(s) enregistrée(s)")
            views.setTextViewText(R.id.widget_latest, latestTitle)
            val open = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}

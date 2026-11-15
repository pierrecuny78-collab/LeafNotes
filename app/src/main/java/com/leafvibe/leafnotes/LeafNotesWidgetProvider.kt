package com.leafvibe.leafnotes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONArray

class LeafNotesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val notes = runCatching {
            val array = JSONArray(context.getSharedPreferences("leafnotes", Context.MODE_PRIVATE).getString("notes", "[]"))
            (0 until array.length()).map { array.getJSONObject(it) }
        }.getOrDefault(emptyList())
        val latest = notes.maxByOrNull { it.optLong("updatedAt", 0L) }
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.leafnotes_widget)
            views.setTextViewText(R.id.widget_title, latest?.optString("title")?.takeIf { it.isNotBlank() } ?: "LeafNotes")
            views.setTextViewText(R.id.widget_body, latest?.optString("body")?.take(140) ?: "Aucune note pour le moment")
            views.setTextViewText(R.id.widget_category, latest?.optString("category")?.let { "📁 $it" } ?: "Touchez pour ouvrir")
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LeafNotesWidgetProvider::class.java))
            if (ids.isNotEmpty()) LeafNotesWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}

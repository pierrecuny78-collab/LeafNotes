package com.leafvibe.leafnotes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class LeafNoteReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("note_title") ?: "Rappel LeafNotes"
        val body = inputData.getString("note_body").orEmpty().ifBlank { "Tu avais prévu de revoir cette note." }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "leafnotes_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Rappels LeafNotes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Rappels programmés pour tes notes et révisions"
                }
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            manager.notify(inputData.getString("note_id")?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        }
    }
}

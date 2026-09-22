package com.example.idate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.idate.MainActivity
import com.example.idate.R

class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_MATCHES = "idate_matches_channel"
        const val CHANNEL_ROOM_EVENTS = "idate_room_channel"
        const val CHANNEL_REMINDERS = "idate_reminders_channel"

        const val MATCH_NOTIF_ID = 1001
        const val ROOM_NOTIF_ID = 1002
        const val SYNC_NOTIF_ID = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val matchChannel = NotificationChannel(
                CHANNEL_MATCHES,
                "Coincidencias (Matches)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando ambos coinciden en un plan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }

            val roomChannel = NotificationChannel(
                CHANNEL_ROOM_EVENTS,
                "Salas en Vivo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de eventos de la sala en tiempo real"
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Recordatorios de Citas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios de planes guardados y sincronización"
            }

            notificationManager.createNotificationChannels(
                listOf(matchChannel, roomChannel, remindersChannel)
            )
        }
    }

    fun showMatchNotification(planTitle: String, partnerName: String = "Tu pareja") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MATCHES)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("💖 ¡MATCH EN TIEMPO REAL!")
            .setContentText("A $partnerName y a ti les encantó: $planTitle")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("¡Coincidencia perfecta! Ambos han votado positivamente por '$planTitle'. ¡Es hora de agendar su cita!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(MATCH_NOTIF_ID, notification)
    }

    fun showPartnerJoinedNotification(partnerName: String, roomCode: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ROOM_EVENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎉 $partnerName se ha unido")
            .setContentText("¡Tu sala $roomCode está lista para empezar a deslizar planes!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ROOM_NOTIF_ID, notification)
    }

    fun showSyncSuccessNotification(plansCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("✨ Planes Sincronizados")
            .setContentText("Se han actualizado $plansCount planes de citas en tu catálogo local.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SYNC_NOTIF_ID, notification)
    }
}

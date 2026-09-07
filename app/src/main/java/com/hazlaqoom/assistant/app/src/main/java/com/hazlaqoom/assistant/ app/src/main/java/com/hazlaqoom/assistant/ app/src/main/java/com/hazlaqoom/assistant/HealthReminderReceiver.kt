package com.hazlaqoom.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.*

class HealthReminderReceiver : BroadcastReceiver(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var pendingMessage: String? = null
    private var context: Context? = null

    companion object {
        const val CHANNEL_ID = "health_reminders"
        const val NOTIFICATION_ID_WATER = 100
        const val NOTIFICATION_ID_HEALTH = 101
        const val ACTION_WATER = "com.hazlaqoom.REMIND_WATER"
        const val ACTION_HEALTH = "com.hazlaqoom.REMIND_HEALTH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        val action = intent.action
        val message = intent.getStringExtra("message") ?: return

        pendingMessage = message
        textToSpeech = TextToSpeech(context, this)

        showNotification(context, message, action)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ar", "SA"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.US
            }

            pendingMessage?.let { msg ->
                textToSpeech?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun showNotification(context: Context, message: String, action: String?) {
        createNotificationChannel(context)

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = when (action) {
            ACTION_WATER -> NOTIFICATION_ID_WATER
            ACTION_HEALTH -> NOTIFICATION_ID_HEALTH
            else -> 999
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Hazlaqoom - Health Reminder")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Voice reminders for water and health check"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

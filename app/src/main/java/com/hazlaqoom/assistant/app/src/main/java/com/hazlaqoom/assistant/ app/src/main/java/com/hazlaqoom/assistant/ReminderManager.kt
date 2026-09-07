package com.hazlaqoom.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.*

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val PREFS_NAME = "HazlaqoomPrefs"
        const val KEY_WATER_ENABLED = "water_reminder_enabled"
        const val KEY_HEALTH_ENABLED = "health_reminder_enabled"
        const val KEY_WATER_INTERVAL = "water_interval_hours"
        const val KEY_HEALTH_HOUR = "health_hour"
        const val KEY_HEALTH_MINUTE = "health_minute"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun scheduleWaterReminders(intervalHours: Int = 2) {
        cancelReminder(HealthReminderReceiver.ACTION_WATER)

        val intent = Intent(context, HealthReminderReceiver::class.java).apply {
            action = HealthReminderReceiver.ACTION_WATER
            putExtra("message", "تذكير: لا تنسى شرب الماء! جسمك يحتاج للترطيب")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = intervalHours * 60 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + intervalMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    intervalMillis,
                    pendingIntent
                )
            } else {
                requestExactAlarmPermission()
            }
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
            )
        }

        prefs.edit().apply {
            putBoolean(KEY_WATER_ENABLED, true)
            putInt(KEY_WATER_INTERVAL, intervalHours)
            apply()
        }
    }

    fun scheduleHealthCheck(hour: Int = 20, minute: Int = 0) {
        cancelReminder(HealthReminderReceiver.ACTION_HEALTH)

        val intent = Intent(context, HealthReminderReceiver::class.java).apply {
            action = HealthReminderReceiver.ACTION_HEALTH
            putExtra("message", "مساء الخير! هل تشتكي من شيء اليوم؟ تذكر أن صحتك تهمنا")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                requestExactAlarmPermission()
            }
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        prefs.edit().apply {
            putBoolean(KEY_HEALTH_ENABLED, true)
            putInt(KEY_HEALTH_HOUR, hour)
            putInt(KEY_HEALTH_MINUTE, minute)
            apply()
        }
    }

    fun cancelWaterReminders() {
        cancelReminder(HealthReminderReceiver.ACTION_WATER)
        prefs.edit().putBoolean(KEY_WATER_ENABLED, false).apply()
    }

    fun cancelHealthCheck() {
        cancelReminder(HealthReminderReceiver.ACTION_HEALTH)
        prefs.edit().putBoolean(KEY_HEALTH_ENABLED, false).apply()
    }

    private fun cancelReminder(action: String) {
        val intent = Intent(context, HealthReminderReceiver::class.java).apply {
            this.action = action
        }
        val requestCode = if (action == HealthReminderReceiver.ACTION_WATER) 100 else 101
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun isWaterReminderEnabled(): Boolean = prefs.getBoolean(KEY_WATER_ENABLED, false)
    fun isHealthReminderEnabled(): Boolean = prefs.getBoolean(KEY_HEALTH_ENABLED, false)
    fun getWaterInterval(): Int = prefs.getInt(KEY_WATER_INTERVAL, 2)
    fun getHealthTime(): Pair<Int, Int> {
        return Pair(prefs.getInt(KEY_HEALTH_HOUR, 20), prefs.getInt(KEY_HEALTH_MINUTE, 0))
    }
}

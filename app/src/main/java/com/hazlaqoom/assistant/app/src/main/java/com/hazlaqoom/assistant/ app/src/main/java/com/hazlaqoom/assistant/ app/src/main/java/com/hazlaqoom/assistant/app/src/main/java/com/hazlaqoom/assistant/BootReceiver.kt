package com.hazlaqoom.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val manager = ReminderManager(context)
            if (manager.isWaterReminderEnabled()) {
                manager.scheduleWaterReminders(manager.getWaterInterval())
            }
            if (manager.isHealthReminderEnabled()) {
                val (hour, minute) = manager.getHealthTime()
                manager.scheduleHealthCheck(hour, minute)
            }
        }
    }
}

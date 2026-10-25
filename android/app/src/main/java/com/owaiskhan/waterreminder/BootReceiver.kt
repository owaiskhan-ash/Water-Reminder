package com.owaiskhan.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms do not survive a reboot, an app update or a time zone change, so the
 * reminder is re-armed whenever one of those happens.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = ReminderPrefs(context)
        if (prefs.enabled) {
            ReminderScheduler.schedule(context.applicationContext)
        }
    }
}

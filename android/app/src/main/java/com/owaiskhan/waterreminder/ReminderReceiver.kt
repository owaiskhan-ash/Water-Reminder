package com.owaiskhan.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires when the reminder alarm goes off: shows the notification and then
 * schedules the following one.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.show(context)
        ReminderScheduler.schedule(context.applicationContext)
    }
}

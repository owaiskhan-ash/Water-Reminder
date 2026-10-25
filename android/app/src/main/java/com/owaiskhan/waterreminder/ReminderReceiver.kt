package com.owaiskhan.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.math.max

/**
 * Fires when the reminder alarm goes off: shows the notification and then
 * schedules the following one.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.show(context)

        // Count on from the moment the alarm was supposed to fire rather than
        // from now, so an alarm that fires a moment early cannot schedule the
        // same slot twice.
        val scheduledFor = intent?.getLongExtra(ReminderScheduler.EXTRA_TRIGGER_AT, 0L) ?: 0L
        val from = max(System.currentTimeMillis(), scheduledFor)

        ReminderScheduler.schedule(context.applicationContext, from)
    }
}

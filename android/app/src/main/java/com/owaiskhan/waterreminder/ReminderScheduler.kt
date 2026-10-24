package com.owaiskhan.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId

/**
 * Schedules the repeating reminder with [AlarmManager].
 *
 * The alarm is a single one-shot exact alarm; every time it fires the receiver
 * schedules the following one. That keeps the reminders aligned to the wall
 * clock even across Doze, device reboots and time zone changes.
 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.owaiskhan.waterreminder.REMIND"

    private const val REQUEST_CODE = 4201

    /** Schedules (or re-schedules) the next reminder. */
    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val prefs = ReminderPrefs(appContext)

        if (!prefs.enabled) {
            cancel(appContext)
            return
        }

        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return

        val triggerAt = ReminderSchedule.nextTrigger(
            nowMillis = System.currentTimeMillis(),
            intervalMinutes = prefs.intervalMinutes,
            quietStartMinute = prefs.quietStartMinute,
            quietEndMinute = prefs.quietEndMinute,
            zone = ZoneId.systemDefault(),
        )

        val pendingIntent = reminderPendingIntent(appContext)

        try {
            if (canScheduleExact(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                // Without the exact-alarm permission we fall back to an inexact
                // alarm. It still fires, the OS just decides the exact moment.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }

        prefs.nextTriggerAt = triggerAt
    }

    /** Cancels any pending reminder. */
    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(reminderPendingIntent(appContext))
        ReminderPrefs(appContext).nextTriggerAt = 0L
    }

    /** True when the OS will let us schedule exact (punctual) alarms. */
    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return canScheduleExact(alarmManager)
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

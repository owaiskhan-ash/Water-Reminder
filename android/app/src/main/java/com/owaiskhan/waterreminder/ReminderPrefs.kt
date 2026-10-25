package com.owaiskhan.waterreminder

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around [SharedPreferences] holding every user preference that
 * affects *when* a reminder fires. Nothing about water intake itself is
 * tracked — this app only ever sends a notification.
 */
class ReminderPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** Master switch. When off, no alarm is ever scheduled. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Length of one reminder interval, in minutes. */
    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, ReminderSchedule.DEFAULT_INTERVAL_MINUTES)
        set(value) = prefs.edit().putInt(KEY_INTERVAL, value).apply()

    /** Whether the quiet window is honoured. */
    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_ENABLED, value).apply()

    /** Quiet window start, minutes past local midnight. */
    var quietStartMinute: Int
        get() = prefs.getInt(KEY_QUIET_START, DEFAULT_QUIET_START)
        set(value) = prefs.edit().putInt(KEY_QUIET_START, value).apply()

    /** Quiet window end, minutes past local midnight. */
    var quietEndMinute: Int
        get() = prefs.getInt(KEY_QUIET_END, DEFAULT_QUIET_END)
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value).apply()

    /** Epoch millis of the next scheduled reminder (0 when nothing is scheduled). */
    var nextTriggerAt: Long
        get() = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        set(value) = prefs.edit().putLong(KEY_NEXT_TRIGGER, value).apply()

    /** True when the quiet window is actually in force. */
    val quietHoursActive: Boolean
        get() = quietHoursEnabled && quietStartMinute != quietEndMinute

    companion object {
        private const val NAME = "water_reminder"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_INTERVAL = "interval_minutes"
        private const val KEY_QUIET_ENABLED = "quiet_enabled"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_NEXT_TRIGGER = "next_trigger_at"

        const val DEFAULT_QUIET_START = 22 * 60   // 22:00
        const val DEFAULT_QUIET_END = 8 * 60      // 08:00
    }
}

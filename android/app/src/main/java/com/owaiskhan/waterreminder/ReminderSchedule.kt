package com.owaiskhan.waterreminder

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure scheduling maths for the reminder. Deliberately free of Android types so
 * it can be unit tested on the JVM.
 *
 * Model
 * -----
 * Reminder slots repeat every [intervalMinutes] and are aligned to local
 * midnight, so with a two hour interval the slots land on 00:00, 02:00, 04:00
 * and so on — the wall clock time stays predictable no matter when the user
 * switches the feature on.
 *
 * Any slot that falls inside the quiet window is skipped entirely: no alarm is
 * scheduled for it, so nothing is delivered and nothing is queued up.
 */
object ReminderSchedule {

    /** Shortest interval we allow, in minutes. */
    const val MIN_INTERVAL_MINUTES = 15

    /** Default interval — every two hours. */
    const val DEFAULT_INTERVAL_MINUTES = 120

    const val MINUTES_PER_DAY = 1440

    /** Minutes past local midnight for the given instant. */
    fun minuteOfDay(millis: Long, zone: ZoneId): Int {
        val zoned = Instant.ofEpochMilli(millis).atZone(zone)
        return zoned.hour * 60 + zoned.minute
    }

    /**
     * True when [minute] falls inside the quiet window.
     *
     * The window is half open — `[quietStart, quietEnd)`. A window that wraps
     * past midnight (22:00 -> 08:00) is supported. When start equals end the
     * window is considered disabled.
     */
    fun inQuietHours(minute: Int, quietStart: Int, quietEnd: Int): Boolean {
        if (quietStart == quietEnd) return false
        val m = ((minute % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val start = ((quietStart % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val end = ((quietEnd % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return if (start < end) m in start until end else m >= start || m < end
    }

    /**
     * The next moment a reminder should fire, in epoch millis.
     *
     * The returned slot is always strictly in the future, always aligned to the
     * interval, and never inside the quiet window.
     */
    fun nextTrigger(
        nowMillis: Long,
        intervalMinutes: Int,
        quietStartMinute: Int,
        quietEndMinute: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val interval = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)
        val now: ZonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zone)

        var dayStart = now.toLocalDate().atStartOfDay(zone)
        var minute = 0

        // A slot is never skipped for more than one full day, so scanning three
        // days is more than enough head-room.
        val maxSlots = 3 * (MINUTES_PER_DAY / interval) + 2

        repeat(maxSlots) {
            val slot = dayStart.plusMinutes(minute.toLong())
            val slotMillis = slot.toInstant().toEpochMilli()

            if (slotMillis > nowMillis &&
                !inQuietHours(minute, quietStartMinute, quietEndMinute)
            ) {
                return slotMillis
            }

            minute += interval
            if (minute >= MINUTES_PER_DAY) {
                minute = 0
                dayStart = dayStart.plusDays(1)
            }
        }

        // Unreachable in practice — a quiet window can never cover a whole day
        // because start == end disables it — but keep a sane fallback.
        return nowMillis + interval * 60_000L
    }
}

package com.owaiskhan.waterreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The reminder only ever sends a notification, so the scheduling maths is the
 * one piece of real logic in the app — and it is covered here.
 */
class ReminderScheduleTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun slotOf(millis: Long): ZonedDateTime =
        Instant.ofEpochMilli(millis).atZone(zone)

    @Test
    fun `next slot is aligned to the interval`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 10, 15),
            intervalMinutes = 120,
            quietStartMinute = 22 * 60,
            quietEndMinute = 8 * 60,
            zone = zone,
        )

        val slot = slotOf(next)
        assertEquals(12, slot.hour)
        assertEquals(0, slot.minute)
        assertEquals(1, slot.dayOfMonth)
    }

    @Test
    fun `an exact slot advances by a whole interval`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 10, 0),
            intervalMinutes = 120,
            quietStartMinute = 22 * 60,
            quietEndMinute = 8 * 60,
            zone = zone,
        )

        assertEquals(12, slotOf(next).hour)
    }

    @Test
    fun `one hour interval lands on the hour`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 9, 5),
            intervalMinutes = 60,
            quietStartMinute = 22 * 60,
            quietEndMinute = 8 * 60,
            zone = zone,
        )

        assertEquals(10, slotOf(next).hour)
    }

    @Test
    fun `slots inside the quiet window are skipped`() {
        // 21:30 -> the 22:00 slot is inside the quiet window, so the next
        // reminder is the first one after quiet hours end.
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 21, 30),
            intervalMinutes = 120,
            quietStartMinute = 22 * 60,
            quietEndMinute = 8 * 60,
            zone = zone,
        )

        val slot = slotOf(next)
        assertEquals(2, slot.dayOfMonth)
        assertEquals(8, slot.hour)
        assertEquals(0, slot.minute)
    }

    @Test
    fun `a slot just before the quiet window is still delivered`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 19, 30),
            intervalMinutes = 120,
            quietStartMinute = 22 * 60,
            quietEndMinute = 8 * 60,
            zone = zone,
        )

        assertEquals(20, slotOf(next).hour)
    }

    @Test
    fun `quiet hours can be disabled entirely`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 21, 30),
            intervalMinutes = 120,
            quietStartMinute = 22 * 60,
            quietEndMinute = 22 * 60, // start == end disables the window
            zone = zone,
        )

        assertEquals(22, slotOf(next).hour)
    }

    @Test
    fun `the schedule rolls over midnight`() {
        val next = ReminderSchedule.nextTrigger(
            nowMillis = at(2024, 5, 1, 23, 10),
            intervalMinutes = 120,
            quietStartMinute = 0,
            quietEndMinute = 0,
            zone = zone,
        )

        val slot = slotOf(next)
        assertEquals(2, slot.dayOfMonth)
        assertEquals(0, slot.hour)
    }

    @Test
    fun `quiet window detection handles a window that wraps midnight`() {
        assertTrue(ReminderSchedule.inQuietHours(23 * 60, 22 * 60, 8 * 60))
        assertTrue(ReminderSchedule.inQuietHours(2 * 60, 22 * 60, 8 * 60))
        assertFalse(ReminderSchedule.inQuietHours(8 * 60, 22 * 60, 8 * 60))
        assertFalse(ReminderSchedule.inQuietHours(12 * 60, 22 * 60, 8 * 60))
    }

    @Test
    fun `quiet window detection handles a window inside one day`() {
        assertTrue(ReminderSchedule.inQuietHours(13 * 60, 12 * 60, 15 * 60))
        assertFalse(ReminderSchedule.inQuietHours(15 * 60, 12 * 60, 15 * 60))
        assertFalse(ReminderSchedule.inQuietHours(11 * 60, 12 * 60, 15 * 60))
    }

    @Test
    fun `the returned slot is always in the future`() {
        val now = at(2024, 5, 1, 14, 37)
        val next = ReminderSchedule.nextTrigger(now, 120, 22 * 60, 8 * 60, zone)

        assertTrue(next > now)
    }

    @Test
    fun `consecutive slots are exactly one interval apart`() {
        var cursor = at(2024, 5, 1, 9, 0)
        repeat(5) {
            cursor = ReminderSchedule.nextTrigger(cursor, 120, 0, 0, zone)
        }

        val first = at(2024, 5, 1, 9, 0)
        val sixth = cursor
        assertEquals(5 * 120L * 60_000L, sixth - first)
    }
}

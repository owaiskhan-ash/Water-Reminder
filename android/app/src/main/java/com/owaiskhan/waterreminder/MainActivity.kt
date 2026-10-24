package com.owaiskhan.waterreminder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.owaiskhan.waterreminder.databinding.ActivityMainBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The single screen of the app.
 *
 * It shows when the next reminder is due and lets the user tune the interval
 * and the quiet window. There is no intake logging on purpose — the app only
 * ever sends a notification.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val prefs by lazy { ReminderPrefs(this) }

    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            renderCountdown()
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    /** Guards the listeners while the views are being populated from prefs. */
    private var bindingViews = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                ReminderScheduler.schedule(this)
            } else {
                prefs.enabled = false
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
            render()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.ensureChannel(this)
        setUpControls()
        render()

        // Keep the alarm in step with the current preferences.
        ReminderScheduler.schedule(this)

        // Ask for notification permission up front — without it a reminder app
        // cannot do its one job.
        if (prefs.enabled && !NotificationHelper.canPostNotifications(this)) {
            requestNotificationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        ReminderScheduler.schedule(this)
        render()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun setUpControls() {
        // Master switch ---------------------------------------------------
        binding.switchReminders.setOnCheckedChangeListener { _, isChecked ->
            if (bindingViews) return@setOnCheckedChangeListener
            prefs.enabled = isChecked
            if (isChecked) {
                if (!NotificationHelper.canPostNotifications(this)) {
                    requestNotificationPermission()
                } else {
                    ReminderScheduler.schedule(this)
                }
            } else {
                ReminderScheduler.cancel(this)
            }
            render()
        }

        // Interval --------------------------------------------------------
        binding.chipGroupInterval.setOnCheckedStateChangeListener { _, checkedIds ->
            if (bindingViews) return@setOnCheckedStateChangeListener
            val minutes = when (checkedIds.firstOrNull()) {
                R.id.chip1h -> 60
                R.id.chip3h -> 180
                R.id.chip4h -> 240
                else -> ReminderSchedule.DEFAULT_INTERVAL_MINUTES
            }
            if (prefs.intervalMinutes != minutes) {
                prefs.intervalMinutes = minutes
                ReminderScheduler.schedule(this)
            }
            render()
        }

        // Quiet hours -----------------------------------------------------
        binding.switchQuietHours.setOnCheckedChangeListener { _, isChecked ->
            if (bindingViews) return@setOnCheckedChangeListener
            prefs.quietHoursEnabled = isChecked
            ReminderScheduler.schedule(this)
            render()
        }

        // Precise alarm opt-in --------------------------------------------
        binding.textExactAlarm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Pushes the current state into the views. */
    private fun render() {
        bindingViews = true

        binding.switchReminders.isChecked = prefs.enabled
        binding.switchQuietHours.isChecked = prefs.quietHoursEnabled
        binding.chipGroupInterval.check(
            when (prefs.intervalMinutes) {
                60 -> R.id.chip1h
                180 -> R.id.chip3h
                240 -> R.id.chip4h
                else -> R.id.chip2h
            },
        )

        val exactAllowed = ReminderScheduler.canScheduleExact(this)
        binding.textExactAlarm.visibility = if (exactAllowed) View.GONE else View.VISIBLE

        binding.textIntervalSummary.text = upcomingSummary()

        bindingViews = false

        renderCountdown()
    }

    /** The next few reminder times, so the schedule is never a mystery. */
    private fun upcomingSummary(): String {
        if (!prefs.enabled) return getString(R.string.reminders_off)

        val zone = ZoneId.systemDefault()
        var cursor = System.currentTimeMillis()
        val upcoming = ArrayList<String>(3)

        repeat(3) {
            cursor = ReminderSchedule.nextTrigger(
                nowMillis = cursor + 1_000L,
                intervalMinutes = prefs.intervalMinutes,
                quietStartMinute = prefs.quietStartMinute,
                quietEndMinute = prefs.quietEndMinute,
                zone = zone,
            )
            upcoming += TIME_FORMAT.format(Instant.ofEpochMilli(cursor).atZone(zone))
        }

        return getString(R.string.upcoming_summary, upcoming.joinToString("  ·  "))
    }

    private fun renderCountdown() {
        val next = prefs.nextTriggerAt
        if (!prefs.enabled || next <= 0L) {
            binding.textNextTime.setText(R.string.reminders_off)
            binding.textCountdown.setText(R.string.em_dash)
            return
        }

        val zoned = Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault())
        binding.textNextTime.text = TIME_FORMAT.format(zoned)
        binding.textCountdown.text = describeDuration(next - System.currentTimeMillis())
    }

    private fun describeDuration(millis: Long): String {
        if (millis <= 0L) return getString(R.string.due_now)
        val totalMinutes = (millis / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours <= 0 -> getString(R.string.in_minutes, minutes)
            minutes == 0 -> getString(R.string.in_hours, hours)
            else -> getString(R.string.in_hours_minutes, hours, minutes)
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 15_000L
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }
}

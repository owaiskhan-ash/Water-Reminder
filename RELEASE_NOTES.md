# Water Reminder v1.0.0

A water intake reminder that does exactly one thing: **sends a notification every
two hours.** No intake logging, no counters, no charts — just the nudge.

## Install

Download `Water-Reminder-v1.0.0.apk` from the assets below and install it on
your phone. You may need to allow your file manager to "install unknown apps".

## First run

1. Open the app and allow **notifications** — without them a reminder app cannot
   deliver anything.
2. Reminders are switched on by default and fire every **2 hours**, aligned to
   your local clock (08:00, 10:00, 12:00 …).
3. Tweak the interval (1 h / 2 h / 3 h / 4 h) or switch on **quiet hours**
   (10 PM – 8 AM) from the single screen.

## Notes

- Reminders use an exact alarm, so they arrive on time even in Doze mode.
- The schedule re-arms itself after a reboot, an app update or a time zone
  change.
- Slots that land inside quiet hours are skipped entirely — nothing is queued
  up and delivered later.
- The app makes no network requests, collects no analytics and needs no account.

## Verifying the build

Every release is built and signed in CI. The workflow runs the unit tests,
assembles the APK, verifies its signature and manifest, and only then attaches
it to this release.

---

**Stack:** Kotlin · min SDK 24 (Android 7.0) · target SDK 34 · Material 3 ·
`AlarmManager` · SharedPreferences · zero dependencies beyond AndroidX.

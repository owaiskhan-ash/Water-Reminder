package com.owaiskhan.waterreminder

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the home screen off-device so the felt themed UI can be reviewed
 * (and shipped with the release) without installing the app.
 *
 * The PNG lands in `app/build/screenshots/home.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppScreenshotTest {

    @Test
    fun renderHomeScreen() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // A deterministic, pleasant state to photograph.
        ReminderPrefs(app).apply {
            enabled = true
            intervalMinutes = ReminderSchedule.DEFAULT_INTERVAL_MINUTES
            quietHoursEnabled = false
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val decor = activity.window.decorView

        val width = 1233   // 411dp at xxhdpi
        val height = 2673  // 891dp at xxhdpi

        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))

        val directory = File("build/screenshots").apply { mkdirs() }
        val file = File(directory, "home.png")
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed" }
        }

        check(file.length() > 0) { "Screenshot was not written" }
        println("screenshot written to ${file.absolutePath} (${file.length()} bytes)")
    }
}

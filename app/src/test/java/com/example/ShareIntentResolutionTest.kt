package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.util.YouTubeUrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShareIntentResolutionTest {

    @Test
    fun testActionSendResolvesToMainActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/dQw4w9WgXcQ")
        }

        val resolvedActivities = packageManager.queryIntentActivities(sendIntent, 0)
        assertTrue("Rankify MainActivity should resolve ACTION_SEND text/plain", resolvedActivities.isNotEmpty())

        val matched = resolvedActivities.firstOrNull {
            it.activityInfo.packageName == context.packageName &&
                    it.activityInfo.name == "com.example.MainActivity"
        }
        assertNotNull("MainActivity must be matched in resolved share activities", matched)
        assertTrue("Activity must be exported", matched!!.activityInfo.exported)
    }

    @Test
    fun testColdStartShareIntentHandling() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Physics Lecture\nhttps://www.youtube.com/watch?v=dQw4w9WgXcQ")
        }

        ActivityScenario.launch<MainActivity>(sendIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                val parsed = YouTubeUrlParser.extractFirstYouTubeUrl(
                    "Physics Lecture\nhttps://www.youtube.com/watch?v=dQw4w9WgXcQ"
                )
                assertEquals("dQw4w9WgXcQ", parsed?.videoId)
            }
        }
    }
}

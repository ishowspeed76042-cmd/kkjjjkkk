package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ApexMapper", appName)
  }

  @Test
  fun `profile repository initializes without crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.ProfileRepository(context)
    val active = repo.activeProfile.value
    assertEquals("Free Fire - Battle Royale", active.name)
    assertEquals(true, repo.profiles.value.isNotEmpty())
  }
}

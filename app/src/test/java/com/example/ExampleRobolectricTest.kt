package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalPreferencesStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.ArStrings
import com.example.ui.state.BuiltinAppId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FlexDesk", appName)
  }

  @Test
  fun `developer mode wp warning state transitions persist properly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = LocalPreferencesStore(context)

    // Turning Developer Mode ON arms WP warning on every boot
    prefs.setDeveloperModeEnabled(true)
    val armed = prefs.localStateFlow.first()
    assertTrue(armed.developerModeEnabled)
    assertFalse(armed.suppressDevBootWarning)

    // Pressing Ctrl+D or waiting 10s silences warning for future boots while staying in Dev Mode
    prefs.setSuppressDevBootWarning(true)
    val silenced = prefs.localStateFlow.first()
    assertTrue(silenced.developerModeEnabled)
    assertTrue(silenced.suppressDevBootWarning)

    // Pressing SPACE re-enables WP and disables Developer Mode
    prefs.setDeveloperModeEnabled(false)
    val reenabled = prefs.localStateFlow.first()
    assertFalse(reenabled.developerModeEnabled)
    assertFalse(reenabled.suppressDevBootWarning)
  }

  @Test
  fun `arabic language lock settings and google web shortcuts work`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = LocalPreferencesStore(context)

    prefs.setAppLanguage(AppLanguage.ARABIC)
    prefs.setLockSettings(pin = "2580", requireOnBoot = true)

    val loaded = prefs.localStateFlow.first()
    assertEquals(AppLanguage.ARABIC, loaded.appLanguage)
    assertTrue(loaded.appLanguage.isArabicResolved())
    assertEquals("2580", loaded.lockPin)
    assertTrue(loaded.requireLockOnBoot)

    assertEquals("https://photos.google.com", BuiltinAppId.GOOGLE_PHOTOS.webUrl)
    assertEquals("صور Google", BuiltinAppId.GOOGLE_PHOTOS.localizedLabel(ArStrings))
  }
}

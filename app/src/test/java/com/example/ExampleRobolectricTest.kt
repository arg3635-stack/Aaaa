package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.LotteryNumberGenerator
import com.example.data.security.SecurityUtils
import com.example.domain.schedule.ScheduleEngine
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
    assertEquals("PeddiLottery", appName)
  }

  @Test
  fun `lottery number generator satisfies format`() {
    // Format: 2 digits + 1 uppercase English letter + 5 digits = 8 characters (e.g. 97G90763)
    val regex = Regex("^[0-9]{2}[A-Z][0-9]{5}$")
    for (i in 0 until 50) {
      val candidate = LotteryNumberGenerator.generateRawCode()
      assertEquals(8, candidate.length)
      assertTrue("Candidate '$candidate' should match regex 2 digits + 1 uppercase letter + 5 digits", candidate.matches(regex))
    }
  }

  @Test
  fun `password hashing with salt is consistent and verifiable`() {
    val password = "SecretPassword123"
    val salt = SecurityUtils.generateSalt()
    val hash = SecurityUtils.hashPassword(password, salt)

    assertTrue(SecurityUtils.verifyPassword(password, salt, hash))
    assertFalse(SecurityUtils.verifyPassword("WrongPassword", salt, hash))
  }

  @Test
  fun `schedule engine formats 12 hour times correctly`() {
    assertEquals("10:00 AM", ScheduleEngine.formatTo12Hour("10:00"))
    assertEquals("6:00 PM", ScheduleEngine.formatTo12Hour("18:00"))
    assertEquals("8:30 PM", ScheduleEngine.formatTo12Hour("20:30"))
    assertEquals("12:00 PM", ScheduleEngine.formatTo12Hour("12:00"))
    assertEquals("12:00 AM", ScheduleEngine.formatTo12Hour("00:00"))
  }
}

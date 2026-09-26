package com.example.domain.schedule

import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

data class ScheduleEvaluation(
    val isOpen: Boolean,
    val statusText: String, // "OPEN" or "CLOSED"
    val timingText: String, // e.g. "10:00 AM – 6:00 PM"
    val nextDrawText: String, // e.g. "Today – 8:00 PM" or "Tomorrow – 8:00 PM"
    val countdownFormatted: String, // e.g. "02:15:35"
    val countdownSeconds: Long,
    val countdownTargetLabel: String, // "Until Closing" or "Until Opening" or "Until Draw"
    val closureReason: String? = null // e.g. "Ticket purchasing is currently closed."
)

object ScheduleEngine {

    const val DEFAULT_TIMEZONE = "Asia/Kolkata"

    fun getCurrentZonedDateTime(timezoneId: String = DEFAULT_TIMEZONE): ZonedDateTime {
        val zone = try {
            ZoneId.of(timezoneId)
        } catch (_: Exception) {
            ZoneId.of(DEFAULT_TIMEZONE)
        }
        return ZonedDateTime.now(zone)
    }

    fun getCurrentDateStr(timezoneId: String = DEFAULT_TIMEZONE): String {
        val zdt = getCurrentZonedDateTime(timezoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return zdt.format(formatter)
    }

    fun getCurrentTimeFormatted(timezoneId: String = DEFAULT_TIMEZONE): String {
        val zdt = getCurrentZonedDateTime(timezoneId)
        val formatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
        return zdt.format(formatter)
    }

    fun formatTo12Hour(time24: String): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val min = parts[1].toInt()
            val amPm = if (hour >= 12) "PM" else "AM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.US, "%d:%02d %s", hour12, min, amPm)
        } catch (_: Exception) {
            time24
        }
    }

    /**
     * Evaluates whether ticket purchasing is OPEN or CLOSED based on:
     * 1. Master admin toggle in settings
     * 2. Schedule active flag
     * 3. Configured opening time, closing time, and current time in the schedule's timezone
     */
    fun evaluate(
        settings: SettingsEntity?,
        schedule: ScheduleEntity?
    ): ScheduleEvaluation {
        // Master switch check
        val masterEnabled = settings?.ticketPurchaseEnabled ?: true
        if (!masterEnabled) {
            return ScheduleEvaluation(
                isOpen = false,
                statusText = "CLOSED",
                timingText = schedule?.let { "${formatTo12Hour(it.openTime)} – ${formatTo12Hour(it.closeTime)}" } ?: "Disabled by Admin",
                nextDrawText = schedule?.let { "Result: ${formatTo12Hour(it.resultTime)}" } ?: "Suspended",
                countdownFormatted = "00:00:00",
                countdownSeconds = 0,
                countdownTargetLabel = "Purchasing Suspended",
                closureReason = "Ticket purchasing has been temporarily paused by administrator."
            )
        }

        if (schedule == null || !schedule.isActive) {
            return ScheduleEvaluation(
                isOpen = false,
                statusText = "CLOSED",
                timingText = "No active schedule",
                nextDrawText = "Check back soon",
                countdownFormatted = "00:00:00",
                countdownSeconds = 0,
                countdownTargetLabel = "Lottery Closed",
                closureReason = "Ticket purchasing is currently closed."
            )
        }

        val tzId = schedule.timezoneId.ifBlank { DEFAULT_TIMEZONE }
        val now = getCurrentZonedDateTime(tzId)

        val schedDate = try {
            LocalDate.parse(schedule.dateStr)
        } catch (_: Exception) {
            now.toLocalDate()
        }

        val openTime = try {
            LocalTime.parse(schedule.openTime)
        } catch (_: Exception) {
            LocalTime.of(10, 0)
        }

        val closeTime = try {
            LocalTime.parse(schedule.closeTime)
        } catch (_: Exception) {
            LocalTime.of(18, 0)
        }

        val resultTime = try {
            LocalTime.parse(schedule.resultTime)
        } catch (_: Exception) {
            LocalTime.of(20, 0)
        }

        val zoneId = ZoneId.of(tzId)
        val openZdt = ZonedDateTime.of(schedDate, openTime, zoneId)
        val closeZdt = ZonedDateTime.of(schedDate, closeTime, zoneId)
        val resultZdt = ZonedDateTime.of(schedDate, resultTime, zoneId)

        val timingText = "${formatTo12Hour(schedule.openTime)} – ${formatTo12Hour(schedule.closeTime)}"
        val isToday = schedDate == now.toLocalDate()
        val nextDrawPrefix = if (isToday) "Today" else schedule.dateStr
        val nextDrawText = "$nextDrawPrefix – ${formatTo12Hour(schedule.resultTime)}"

        val nowEpoch = now.toEpochSecond()
        val openEpoch = openZdt.toEpochSecond()
        val closeEpoch = closeZdt.toEpochSecond()
        val resultEpoch = resultZdt.toEpochSecond()

        return when {
            nowEpoch < openEpoch -> {
                // Not opened yet today
                val diff = openEpoch - nowEpoch
                ScheduleEvaluation(
                    isOpen = false,
                    statusText = "CLOSED",
                    timingText = timingText,
                    nextDrawText = nextDrawText,
                    countdownFormatted = formatDuration(diff),
                    countdownSeconds = diff,
                    countdownTargetLabel = "Opens In",
                    closureReason = "Ticket purchasing opens at ${formatTo12Hour(schedule.openTime)}."
                )
            }
            nowEpoch in openEpoch until closeEpoch -> {
                // Open for ticket purchase!
                val diff = closeEpoch - nowEpoch
                ScheduleEvaluation(
                    isOpen = true,
                    statusText = "OPEN",
                    timingText = timingText,
                    nextDrawText = nextDrawText,
                    countdownFormatted = formatDuration(diff),
                    countdownSeconds = diff,
                    countdownTargetLabel = "Closes In",
                    closureReason = null
                )
            }
            nowEpoch in closeEpoch until resultEpoch -> {
                // Sales closed, waiting for draw result!
                val diff = resultEpoch - nowEpoch
                ScheduleEvaluation(
                    isOpen = false,
                    statusText = "CLOSED",
                    timingText = timingText,
                    nextDrawText = nextDrawText,
                    countdownFormatted = formatDuration(diff),
                    countdownSeconds = diff,
                    countdownTargetLabel = "Draw In",
                    closureReason = "Ticket purchasing is currently closed. Today's draw is at ${formatTo12Hour(schedule.resultTime)}."
                )
            }
            else -> {
                // Past result time for this schedule
                ScheduleEvaluation(
                    isOpen = false,
                    statusText = "CLOSED",
                    timingText = timingText,
                    nextDrawText = nextDrawText,
                    countdownFormatted = "00:00:00",
                    countdownSeconds = 0,
                    countdownTargetLabel = "Draw Completed",
                    closureReason = "Ticket purchasing is currently closed."
                )
            }
        }
    }

    fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "00:00:00"
        val hours = totalSeconds / 3600
        val remainder = totalSeconds % 3600
        val minutes = remainder / 60
        val seconds = remainder % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}

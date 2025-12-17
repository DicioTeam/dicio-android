package org.stypox.dicio.skills.calendar

import android.content.Intent
import android.provider.CalendarContract
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Calendar
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CalendarSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Calendar>
) : StandardRecognizerSkill<Calendar>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Calendar): SkillOutput {
        return when (inputData) {
            is Calendar.CreateEvent -> createEvent(ctx, inputData)
            is Calendar.QueryEvents -> queryEvents(ctx, inputData)
        }
    }

    private fun createEvent(ctx: SkillContext, inputData: Calendar.CreateEvent): SkillOutput {
        val title = inputData.title
        val dateTimeStr = inputData.dateTime
        val durationInput = inputData.duration

        // Validate title
        if (title.isNullOrBlank()) {
            return CalendarOutput.NoTitle
        }

        val npf = ctx.parserFormatter
        var cleanTitle = title.trim()
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        var extractedDateTime: LocalDateTime? = null

        // First, try to parse date/time from the explicit dateTimeStr capture
        if (!dateTimeStr.isNullOrBlank() && npf != null) {
            extractedDateTime = npf.extractDateTime(dateTimeStr)
                .now(LocalDateTime.now())
                .preferMonthBeforeDay(false)
                .first
        }

        // If no date/time found in explicit capture, check if the title contains date/time info
        if (extractedDateTime == null && npf != null) {
            val mixedResult = npf.extractDateTime(cleanTitle)
                .now(LocalDateTime.now())
                .preferMonthBeforeDay(false)
                .mixedWithText
            
            // Check if we found a date/time and extract the first one
            var foundDateTime = false
            val titleParts = mutableListOf<String>()
            
            for (item in mixedResult) {
                when (item) {
                    is LocalDateTime -> {
                        if (!foundDateTime) {
                            extractedDateTime = item
                            foundDateTime = true
                        }
                        // Don't add LocalDateTime to titleParts - we're removing it
                    }
                    is String -> titleParts.add(item)
                }
            }
            
            // Only update the title if we actually found a date/time to remove
            if (foundDateTime) {
                val reconstructedTitle = titleParts.joinToString("").trim()
                if (reconstructedTitle.isNotBlank()) {
                    cleanTitle = reconstructedTitle
                }
            }
        }

        // Default to current time if still no date/time found
        val startDateTime = extractedDateTime ?: LocalDateTime.now()

        // Parse duration or default to 1 hour
        val durationMillis: Long = if (durationInput != null && npf != null) {
            val parsedDuration = npf.extractDuration(durationInput)
                .first?.toJavaDuration()
            parsedDuration?.toMillis() ?: (60 * 60 * 1000L) // default 1 hour
        } else {
            60 * 60 * 1000L // default 1 hour (in milliseconds)
        }

        // Calculate start and end times in milliseconds
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + durationMillis

        // Create calendar intent
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, cleanTitle)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Check if there's an app that can handle the calendar intent
        val packageManager = ctx.android.packageManager
        val canHandleIntent = calendarIntent.resolveActivity(packageManager) != null

        return if (canHandleIntent) {
            ctx.android.startActivity(calendarIntent)
            CalendarOutput.Success(cleanTitle, startDateTime, durationMillis)
        } else {
            CalendarOutput.NoCalendarApp
        }
    }

    private fun queryEvents(ctx: SkillContext, inputData: Calendar.QueryEvents): SkillOutput {
        val dateTimeStr = inputData.dateTime
        val npf = ctx.parserFormatter

        // Parse the date/time or default to today
        val parsedDateTime: LocalDateTime = if (!dateTimeStr.isNullOrBlank() && npf != null) {
            npf.extractDateTime(dateTimeStr)
                .now(LocalDateTime.now())
                .preferMonthBeforeDay(false)
                .first ?: LocalDateTime.now()
        } else {
            LocalDateTime.now()
        }

        // Set to start of day for the query (we only care about the date, not the time)
        val startOfDay = parsedDateTime.toLocalDate().atStartOfDay()
        val endOfDay = parsedDateTime.toLocalDate().atTime(LocalTime.MAX)

        // Convert to milliseconds for calendar query
        val startMillis = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Query calendar events using Instances table (handles recurring events properly)
        val events = mutableListOf<CalendarEvent>()
        val contentResolver = ctx.android.contentResolver

        // Build the URI for querying instances in the time range
        val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            appendPath(startMillis.toString())
            appendPath(endMillis.toString())
        }.build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        try {
            contentResolver.query(
                instancesUri,
                projection,
                null, // selection handled by URI
                null, // selectionArgs handled by URI
                sortOrder
            )?.use { cursor ->
                val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locationIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext()) {
                    val title = if (titleIndex != -1) cursor.getString(titleIndex) ?: "Untitled" else "Untitled"
                    val startTimeMillis = if (startIndex != -1) cursor.getLong(startIndex) else continue
                    val endTimeMillis = if (endIndex != -1) cursor.getLong(endIndex) else startTimeMillis + (60 * 60 * 1000)
                    val location = if (locationIndex != -1) cursor.getString(locationIndex) else null
                    val isAllDay = if (allDayIndex != -1) cursor.getInt(allDayIndex) == 1 else false

                    val startDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(startTimeMillis),
                        ZoneId.systemDefault()
                    )
                    val endDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(endTimeMillis),
                        ZoneId.systemDefault()
                    )

                    events.add(CalendarEvent(title, startDateTime, endDateTime, location, isAllDay))
                }
            }
        } catch (e: Exception) {
            // Handle permission or other errors gracefully
            return CalendarOutput.NoEvents(startOfDay)
        }

        return CalendarOutput.EventsList(events, startOfDay)
    }
}

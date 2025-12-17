package org.stypox.dicio.skills.calendar

import android.content.Intent
import android.provider.CalendarContract
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Calendar
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Calendar>
) : StandardRecognizerSkill<Calendar>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Calendar): SkillOutput {
        val (title, dateTimeStr, durationInput) = when (inputData) {
            is Calendar.CreateEvent -> Triple(
                inputData.title,
                inputData.dateTime,
                inputData.duration
            )
        }

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
}

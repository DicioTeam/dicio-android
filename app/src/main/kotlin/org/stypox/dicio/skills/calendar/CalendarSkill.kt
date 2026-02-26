package org.stypox.dicio.skills.calendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances as CCI
import android.util.Log
import java.time.Duration
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Calendar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.stypox.dicio.R
import org.stypox.dicio.util.getString


class CalendarSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Calendar>
) : StandardRecognizerSkill<Calendar>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Calendar): SkillOutput {
        return when (inputData) {
            is Calendar.Create -> createEvent(ctx, inputData)
            is Calendar.Query -> queryEvents(ctx, inputData)
        }
    }

    private fun createEvent(ctx: SkillContext, inputData: Calendar.Create): SkillOutput {
        // obtain capturing group data (note that we either have .end. or .duration., never both)
        val title = inputData.title
            ?.trim()
            ?.replaceFirstChar { it.titlecase(ctx.locale) }
            ?: ctx.getString(R.string.skill_calendar_no_name)
        val begin = inputData.begin
            ?: LocalDateTime.now()
        val end = inputData.end
            ?: begin.plus(inputData.duration?.toJavaDuration() ?: Duration.ofHours(1))

        // create calendar intent
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.toEpochMilli())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toEpochMilli())
        }

        // start activity
        return try {
            ctx.android.startActivity(calendarIntent)
            CalendarOutput.Added(title, begin, end)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Could not start calendar activity", e)
            CalendarOutput.NoCalendarApp
        }
    }

    private fun queryEvents(ctx: SkillContext, inputData: Calendar.Query): SkillOutput {
        // we only care about the date, not the time
        val date = inputData.`when`?.toLocalDate() ?: LocalDate.now()
        val events = ArrayList<CalendarEvent>()

        ctx.android.contentResolver.query(
            // query all events from the beginning to the end of the day
            CCI.CONTENT_URI.buildUpon()
                .appendPath(date.atStartOfDay().toEpochMilli().toString())
                .appendPath(date.atTime(LocalTime.MAX).toEpochMilli().toString())
                .build(),
            // we want to read these fields
            arrayOf(CCI.EVENT_ID, CCI.TITLE, CCI.BEGIN, CCI.END, CCI.EVENT_LOCATION, CCI.ALL_DAY),
            null, // selection handled by URI
            null, // selectionArgs handled by URI
            "${CCI.BEGIN} ASC" // order them by begin
        )?.use { cursor ->
            // use ...OrThrow() because all fields surely exist as we requested them in query()
            val eventIdIndex = cursor.getColumnIndexOrThrow(CCI.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CCI.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CCI.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CCI.END)
            val locationIndex = cursor.getColumnIndexOrThrow(CCI.EVENT_LOCATION)
            val allDayIndex = cursor.getColumnIndexOrThrow(CCI.ALL_DAY)

            // move through all rows returned by the query and read the fields
            while (cursor.moveToNext()) {
                events.add(
                    CalendarEvent(
                        id = cursor.getLongOrNull(eventIdIndex),
                        title = cursor.getNonBlankStringOrNull(titleIndex),
                        begin = cursor.getDateTimeOrNull(beginIndex),
                        end = cursor.getDateTimeOrNull(endIndex),
                        location = cursor.getNonBlankStringOrNull(locationIndex),
                        isAllDay = cursor.getBooleanOrFalse(allDayIndex),
                    )
                )
            }
        }

        return CalendarOutput.EventsList(events, date)
    }

    companion object {
        private const val TAG: String = "CalendarSkill"
    }
}

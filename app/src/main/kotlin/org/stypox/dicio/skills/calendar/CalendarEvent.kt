package org.stypox.dicio.skills.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.dicio.skill.context.SkillContext
import org.stypox.dicio.R
import org.stypox.dicio.util.getString

data class CalendarEvent(
    val title: String?,
    val begin: LocalDateTime?,
    val end: LocalDateTime?,
    val location: String?,
    private val isAllDay: Boolean,
) {
    fun isAllDay(queryDate: LocalDate): Boolean {
        if (isAllDay) {
            return true
        }
        return begin?.isBefore(queryDate.atStartOfDay()) == true &&
                end?.isAfter(queryDate.atTime(LocalTime.MAX)) == true
    }

    fun toSpeechString(ctx: SkillContext, queryDate: LocalDate): String {
        val beginFormatted = begin?.toLocalTime()
            ?.let { ctx.parserFormatter?.niceTime(it)?.get() }

        return if (title == null) {
            if (location == null) {
                if (isAllDay(queryDate)) {
                    ctx.getString(R.string.skill_calendar_unnamed_all_day)
                } else if (beginFormatted == null) {
                    ctx.getString(R.string.skill_calendar_unnamed_unknown_time)
                } else {
                    ctx.getString(R.string.skill_calendar_unnamed_begin, beginFormatted)
                }
            } else {
                if (isAllDay(queryDate)) {
                    ctx.getString(R.string.skill_calendar_location_all_day, location)
                } else if (beginFormatted == null) {
                    ctx.getString(R.string.skill_calendar_location_unknown_time, location)
                } else {
                    ctx.getString(R.string.skill_calendar_location_begin, location, beginFormatted)
                }
            }
        } else {
            if (location == null) {
                if (isAllDay(queryDate)) {
                    ctx.getString(R.string.skill_calendar_title_all_day, title)
                } else if (beginFormatted == null) {
                    ctx.getString(R.string.skill_calendar_title_unknown_time, title)
                } else {
                    ctx.getString(R.string.skill_calendar_title_begin, title, beginFormatted)
                }
            } else {
                if (isAllDay(queryDate)) {
                    ctx.getString(R.string.skill_calendar_title_location_all_day, title, location)
                } else if (beginFormatted == null) {
                    ctx.getString(R.string.skill_calendar_title_location_unknown_time, title, location)
                } else {
                    ctx.getString(R.string.skill_calendar_title_location_begin, title, location, beginFormatted)
                }
            }
        }
    }
}

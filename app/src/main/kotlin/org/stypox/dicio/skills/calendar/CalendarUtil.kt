package org.stypox.dicio.skills.calendar

import android.database.Cursor
import android.text.format.DateUtils
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.dicio.skill.context.SkillContext

typealias DicioNumbersDuration = org.dicio.numbers.unit.Duration

internal fun LocalDateTime.toEpochMilli(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

internal fun formatDateTimeRange(ctx: SkillContext, begin: LocalDateTime, end: LocalDateTime): String {
    return DateUtils.formatDateRange(
        ctx.android,
        begin.toEpochMilli(),
        end.toEpochMilli(),
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_ALL
    )
}

internal fun formatDuration(ctx: SkillContext, duration: Duration): String {
    return ctx.parserFormatter
        ?.niceDuration(DicioNumbersDuration(duration))
        ?.speech(false)
        ?.get()
        ?: DateUtils.formatElapsedTime(duration.toSeconds())
}

internal fun formatDateTime(date: LocalDateTime): String {
    return date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG))
}

// some simple helper functions to work with Cursor...
internal fun Cursor.getNonBlankStringOrNull(index: Int): String? {
    return index.takeUnless(this::isNull)?.let(this::getString)?.takeUnless(String::isBlank)
}

internal fun Cursor.getDateTimeOrNull(index: Int): LocalDateTime? {
    val millis = index.takeUnless(this::isNull)?.let(this::getLong) ?: return null
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
}

internal fun Cursor.getBooleanOrFalse(index: Int): Boolean {
    return index.takeUnless(this::isNull)?.let(this::getInt) == 1
}

package org.stypox.dicio.skills.calendar

import java.time.LocalDateTime

data class CalendarEvent(
    val title: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean = false
)

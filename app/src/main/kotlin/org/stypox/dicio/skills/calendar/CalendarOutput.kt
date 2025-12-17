package org.stypox.dicio.skills.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.util.getString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface CalendarOutput : SkillOutput {

    data class Success(
        private val title: String,
        private val startDateTime: LocalDateTime,
        private val durationMillis: Long
    ) : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val formattedDateTime = ctx.parserFormatter?.niceDateTime(startDateTime)?.get()
                ?: startDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
            
            val durationHours = durationMillis / (60 * 60 * 1000)
            val durationMinutes = (durationMillis % (60 * 60 * 1000)) / (60 * 1000)
            
            val durationText = when {
                durationHours > 0 && durationMinutes > 0 -> 
                    ctx.getString(R.string.skill_calendar_duration_hours_minutes, durationHours, durationMinutes)
                durationHours > 0 -> 
                    ctx.getString(R.string.skill_calendar_duration_hours, durationHours)
                durationMinutes > 0 -> 
                    ctx.getString(R.string.skill_calendar_duration_minutes, durationMinutes)
                else -> 
                    ctx.getString(R.string.skill_calendar_duration_hours, 1)
            }
            
            return ctx.getString(R.string.skill_calendar_success, title, formattedDateTime, durationText)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = ctx.getString(R.string.skill_calendar_event_added),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = startDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    data object NoTitle : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_calendar_no_title)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = getSpeechOutput(ctx))
        }
    }

    data class EventsList(
        private val events: List<CalendarEvent>,
        private val queryDate: LocalDateTime
    ) : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            if (events.isEmpty()) {
                return NoEvents(queryDate).getSpeechOutput(ctx)
            }

            val formattedDate = ctx.parserFormatter?.niceDate(queryDate.toLocalDate())?.get()
                ?: queryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

            val maxEventsToRead = 5
            val eventsToRead = if (events.size > maxEventsToRead) events.take(maxEventsToRead) else events
            
            val eventList = eventsToRead.joinToString(", ") { event ->
                if (event.isAllDay) {
                    "${event.title} (${ctx.getString(R.string.skill_calendar_all_day)})"
                } else {
                    val time = ctx.parserFormatter?.niceTime(event.startDateTime.toLocalTime())?.get()
                        ?: event.startDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                    "${event.title} ${ctx.getString(R.string.skill_calendar_event_at)} $time"
                }
            }

            val prefix = if (events.size > maxEventsToRead) {
                ctx.getString(R.string.skill_calendar_on_date_you_have_count, formattedDate, events.size) + ". " +
                ctx.getString(R.string.skill_calendar_here_are_first, maxEventsToRead) + ": "
            } else if (events.size == 1) {
                ctx.getString(R.string.skill_calendar_on_date_you_have_one_event, formattedDate) + ": "
            } else {
                ctx.getString(R.string.skill_calendar_on_date_you_have_count, formattedDate, events.size) + ": "
            }

            return prefix + eventList
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            if (events.isEmpty()) {
                NoEvents(queryDate).GraphicalOutput(ctx)
                return
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateStr = queryDate.format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
                    )
                    
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (events.size == 1) 
                            ctx.getString(R.string.skill_calendar_one_event_found) 
                        else 
                            ctx.getString(R.string.skill_calendar_events_found, events.size),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show event summaries without dates
                    events.forEach { event ->
                        val displayText = if (event.isAllDay) {
                            "${event.title} (${ctx.getString(R.string.skill_calendar_all_day_capitalized)})"
                        } else {
                            val timeStr = event.startDateTime.format(
                                DateTimeFormatter.ofPattern("h:mma", java.util.Locale.getDefault())
                            ).lowercase()
                            "${event.title} @ $timeStr"
                        }
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (event.isAllDay) 
                                            ctx.getString(R.string.skill_calendar_all_day_capitalized) 
                                        else event.startDateTime.format(
                                            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (event.location != null) {
                                        Text(
                                            text = event.location,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    data class NoEvents(
        private val queryDate: LocalDateTime
    ) : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val formattedDate = ctx.parserFormatter?.niceDate(queryDate.toLocalDate())?.get()
                ?: queryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            return ctx.getString(R.string.skill_calendar_no_events, formattedDate)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateStr = queryDate.format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
                )
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = ctx.getString(R.string.skill_calendar_no_events_simple),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    data object NoCalendarApp : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_calendar_no_app)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = getSpeechOutput(ctx))
        }
    }
}

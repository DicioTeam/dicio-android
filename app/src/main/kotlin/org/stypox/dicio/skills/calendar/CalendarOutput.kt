package org.stypox.dicio.skills.calendar

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.di.SkillContextImpl
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.StringUtils
import org.stypox.dicio.util.getPluralString
import org.stypox.dicio.util.getString

// TODO remind me about whatever tomorrow at nine is misinterpreted
sealed interface CalendarOutput : SkillOutput {

    data class Added(
        private val title: String,
        private val begin: LocalDateTime,
        private val end: LocalDateTime
    ) : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val duration = Duration.between(begin, end)
            val beginText = ctx.parserFormatter!!
                .niceDateTime(begin)
                .get()

            return if (duration < Duration.ofHours(20)) {
                val durationText = ctx.parserFormatter!!
                    .niceDuration(DicioNumbersDuration(duration))
                    .speech(true)
                    .get()
                ctx.getString(R.string.skill_calendar_adding_begin_duration, title, beginText, durationText)
            } else {
                val endText = ctx.parserFormatter!!
                    .niceDateTime(end)
                    .get()
                ctx.getString(R.string.skill_calendar_adding_begin_end, title, beginText, endText)
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            val dateRangeFormatted = remember { formatDateTimeRange(ctx, begin, end) }
            val duration = remember { Duration.between(begin, end) }
            val durationFormatted = remember(duration) { formatDuration(ctx, duration) }

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = ctx.getString(R.string.skill_calendar_app_was_instructed),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = dateRangeFormatted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                if (duration >= Duration.ofSeconds(1)) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.skill_calendar_duration, durationFormatted),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    data object NoCalendarApp : CalendarOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_calendar_no_app)
    }

    data class EventsList(
        private val events: List<CalendarEvent>,
        private val queryDate: LocalDate
    ) : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val formattedEvents = events
                .take(MAX_EVENTS_TO_SPEAK)
                .joinToString(", ") { event -> event.toSpeechString(ctx, queryDate) }

            return if (events.size <= MAX_EVENTS_TO_SPEAK) {
                ctx.getPluralString(
                    resId = R.plurals.skill_calendar_on_date_you_have_count,
                    resIdIfZero = R.string.skill_calendar_on_date_you_have_count_zero,
                    quantity = events.size,
                    formattedEvents,
                )
            } else {
                ctx.getPluralString(
                    resId = R.plurals.skill_calendar_on_date_you_have_count_limited,
                    resIdIfZero = R.string.skill_calendar_on_date_you_have_count_zero,
                    quantity = events.size,
                    MAX_EVENTS_TO_SPEAK,
                    formattedEvents
                )
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = queryDate.format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ctx.getPluralString(
                        resId = R.plurals.skill_calendar_events,
                        quantity = events.size,
                        resIdIfZero = R.string.skill_calendar_events_zero
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                for (event in events) {
                    EventCard(
                        ctx = ctx,
                        event = event,
                        queryDate = queryDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        companion object {
            const val MAX_EVENTS_TO_SPEAK = 5
        }
    }
}

@Composable
private fun EventCard(
    ctx: SkillContext,
    event: CalendarEvent,
    queryDate: LocalDate,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = {
            if (event.id == null) {
                return@Card
            }
            // open the full event description in the calendar app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id!!)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.android.startActivity(intent)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = StringUtils.joinNonBlank(event.title, event.location)
                .takeIf(String::isNotEmpty)
                ?: ctx.getString(R.string.skill_calendar_no_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 11.dp, top = 8.dp, end = 11.dp, bottom = 2.dp)
        )
        Text(
            text = when {
                event.isAllDay(queryDate) -> ctx.getString(R.string.skill_calendar_all_day)
                event.begin != null && event.end != null -> formatDateTimeRange(ctx, event.begin, event.end)
                event.end != null -> formatDateTime(event.end)
                event.begin != null -> formatDateTime(event.begin)
                else -> ctx.getString(R.string.skill_calendar_unknown_time)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 11.dp, end = 11.dp, bottom = 8.dp)
        )
    }
}

@Preview
@Composable
private fun EventCardPreview() {
    EventCard(
        ctx = SkillContextImpl.newForPreviews(LocalContext.current),
        event = CalendarEvent(
            id = null,
            title = "Meet with John",
            begin = LocalDateTime.of(2026, 2, 26, 18, 0),
            end = LocalDateTime.of(2026, 2, 26, 21, 0),
            location = "Online",
            isAllDay = false,
        ),
        queryDate = LocalDate.of(2026, 2, 26),
    )
}

package org.stypox.dicio.skills.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = getSpeechOutput(ctx),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    data object NoCalendarApp : CalendarOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_calendar_no_app)

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
                    text = getSpeechOutput(ctx),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

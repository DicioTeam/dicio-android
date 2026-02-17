package org.stypox.dicio.skills.reminder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface ReminderOutput : SkillOutput {

    class Created(
        private val title: String,
        private val dateTime: LocalDateTime?,
    ) : ReminderOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            if (title.isBlank()) {
                return ctx.getString(R.string.skill_reminder_no_task)
            }
            return if (dateTime != null) {
                val formatted = dateTime.format(
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                )
                ctx.getString(R.string.skill_reminder_created_with_date, title, formatted)
            } else {
                ctx.getString(R.string.skill_reminder_created, title)
            }
        }
    }

    data object TasksNotInstalled : ReminderOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_reminder_tasks_not_installed)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Headline(text = getSpeechOutput(ctx))
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=org.tasks")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        ctx.android.startActivity(intent)
                    } catch (_: Exception) {
                        // Play Store not available, try browser
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=org.tasks")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        ctx.android.startActivity(webIntent)
                    }
                }) {
                    Text(
                        text = ctx.getString(R.string.skill_reminder_install_tasks),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

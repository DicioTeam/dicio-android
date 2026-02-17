package org.stypox.dicio.skills.reminder

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.first
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Reminder
import org.stypox.dicio.skills.reminder.ReminderInfo.reminderDataStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReminderSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Reminder>,
) : StandardRecognizerSkill<Reminder>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(
        ctx: SkillContext,
        inputData: Reminder,
    ): SkillOutput {
        val rawTask = when (inputData) {
            is Reminder.Create -> inputData.task?.trim() ?: ""
        }

        if (rawTask.isBlank()) {
            return ReminderOutput.Created(title = "", dateTime = null)
        }

        // Check if Tasks.org is installed
        if (!isTasksOrgInstalled(ctx)) {
            return ReminderOutput.TasksNotInstalled
        }

        // Use extractDateTime with mixedWithText to split the captured text
        // into the task description (text parts) and the date/time
        val parserFormatter = ctx.parserFormatter
        var taskTitle = rawTask
        var dateTime: LocalDateTime? = null

        if (parserFormatter != null) {
            val parts = parserFormatter.extractDateTime(rawTask).mixedWithText
            val textParts = StringBuilder()
            for (part in parts) {
                if (part is LocalDateTime) {
                    dateTime = part
                } else if (part is String) {
                    textParts.append(part)
                }
            }
            val extracted = textParts.toString().trim()
            if (extracted.isNotBlank()) {
                taskTitle = extracted
            }
        }

        // Read settings
        val prefs = ctx.android.reminderDataStore.data.first()
        val priority = toTasksOrgPriority(prefs.defaultPriority)
        val description = if (prefs.saveVoiceInput) rawTask else null

        // Send broadcast to Tasks.org
        sendTaskBroadcast(ctx, taskTitle, dateTime, priority, description)

        return ReminderOutput.Created(title = taskTitle, dateTime = dateTime)
    }

    private fun isTasksOrgInstalled(ctx: SkillContext): Boolean {
        return try {
            ctx.android.packageManager.getPackageInfo(TASKS_ORG_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun toTasksOrgPriority(priority: ReminderPriority): String {
        return when (priority) {
            ReminderPriority.REMINDER_PRIORITY_HIGH -> "0"
            ReminderPriority.REMINDER_PRIORITY_MEDIUM -> "1"
            ReminderPriority.REMINDER_PRIORITY_LOW -> "2"
            ReminderPriority.REMINDER_PRIORITY_NONE -> "3"
            ReminderPriority.UNRECOGNIZED -> "1"
        }
    }

    private fun sendTaskBroadcast(
        ctx: SkillContext,
        title: String,
        dateTime: LocalDateTime?,
        priority: String,
        description: String?,
    ) {
        val taskBundle = Bundle().apply {
            putString(EXTRA_TITLE, title)
            if (dateTime != null) {
                putString(EXTRA_DUE_DATE, dateTime.toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE))
                putString(EXTRA_DUE_TIME, dateTime.toLocalTime()
                    .format(DateTimeFormatter.ISO_LOCAL_TIME))
            }
            putString(EXTRA_PRIORITY, priority)
            if (description != null) {
                putString(EXTRA_DESCRIPTION, description)
            }
            putInt(EXTRA_VERSION_CODE, 1)
        }

        val intent = Intent(ACTION_FIRE_SETTING).apply {
            setPackage(TASKS_ORG_PACKAGE)
            putExtra(EXTRA_BUNDLE, taskBundle)
        }

        ctx.android.sendBroadcast(intent)
    }

    companion object {
        private const val TASKS_ORG_PACKAGE = "org.tasks"
        private const val ACTION_FIRE_SETTING =
            "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        private const val EXTRA_BUNDLE =
            "com.twofortyfouram.locale.intent.extra.BUNDLE"
        private const val EXTRA_TITLE =
            "org.tasks.locale.create.STRING_TITLE"
        private const val EXTRA_DUE_DATE =
            "org.tasks.locale.create.STRING_DUE_DATE"
        private const val EXTRA_DUE_TIME =
            "org.tasks.locale.create.STRING_DUE_TIME"
        private const val EXTRA_PRIORITY =
            "org.tasks.locale.create.STRING_PRIORITY"
        private const val EXTRA_DESCRIPTION =
            "org.tasks.locale.create.STRING_DESCRIPTION"
        private const val EXTRA_VERSION_CODE =
            "org.tasks.locale.create.INT_VERSION_CODE"
    }
}

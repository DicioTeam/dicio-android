package org.stypox.dicio.skills.reminder

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import kotlinx.coroutines.launch
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.ui.BooleanSetting
import org.stypox.dicio.settings.ui.ListSetting

object ReminderInfo : SkillInfo("reminder") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_reminder)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_reminder)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Notifications)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Reminder[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return ReminderSkill(ReminderInfo, Sentences.Reminder[ctx.sentencesLanguage]!!)
    }

    internal val Context.reminderDataStore by dataStore(
        fileName = "skill_settings_reminder.pb",
        serializer = SkillSettingsReminderSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsReminderSerializer.defaultValue
        },
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.reminderDataStore
        val data by dataStore.data.collectAsState(SkillSettingsReminderSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            ListSetting(
                title = stringResource(R.string.pref_reminder_default_priority),
                possibleValues = listOf(
                    ListSetting.Value(
                        value = ReminderPriority.REMINDER_PRIORITY_HIGH,
                        name = stringResource(R.string.pref_reminder_priority_high),
                    ),
                    ListSetting.Value(
                        value = ReminderPriority.REMINDER_PRIORITY_MEDIUM,
                        name = stringResource(R.string.pref_reminder_priority_medium),
                    ),
                    ListSetting.Value(
                        value = ReminderPriority.REMINDER_PRIORITY_LOW,
                        name = stringResource(R.string.pref_reminder_priority_low),
                    ),
                    ListSetting.Value(
                        value = ReminderPriority.REMINDER_PRIORITY_NONE,
                        name = stringResource(R.string.pref_reminder_priority_none),
                    ),
                ),
            ).Render(
                value = when (val priority = data.defaultPriority) {
                    ReminderPriority.UNRECOGNIZED -> ReminderPriority.REMINDER_PRIORITY_MEDIUM
                    else -> priority
                },
                onValueChange = { priority ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().setDefaultPriority(priority).build()
                        }
                    }
                },
            )

            BooleanSetting(
                title = stringResource(R.string.pref_reminder_save_voice_input),
                descriptionOff = stringResource(R.string.pref_reminder_save_voice_input_off),
                descriptionOn = stringResource(R.string.pref_reminder_save_voice_input_on),
            ).Render(
                value = data.saveVoiceInput,
                onValueChange = { save ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().setSaveVoiceInput(save).build()
                        }
                    }
                },
            )
        }
    }
}

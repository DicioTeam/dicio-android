package org.stypox.dicio.skills.nextcloud_notes

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Note
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
import org.stypox.dicio.settings.ui.StringSetting

object NextcloudNotesInfo : SkillInfo("nextcloud_notes") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_nextcloud_notes)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_nextcloud_notes)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Note)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.NextcloudNotes[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return NextcloudNotesSkill(NextcloudNotesInfo, Sentences.NextcloudNotes[ctx.sentencesLanguage]!!)
    }

    // DataStore for Nextcloud Notes settings
    internal val Context.nextcloudNotesDataStore by dataStore(
        fileName = "skill_settings_nextcloud_notes.pb",
        serializer = SkillSettingsNextcloudNotesSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsNextcloudNotesSerializer.defaultValue
        },
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.nextcloudNotesDataStore
        val data by dataStore.data.collectAsState(SkillSettingsNextcloudNotesSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            StringSetting(
                title = stringResource(R.string.pref_nextcloud_notes_server_address),
                descriptionWhenEmpty = stringResource(R.string.pref_nextcloud_notes_server_address_hint),
            ).Render(
                value = data.serverAddress,
                onValueChange = { serverAddress ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setServerAddress(serverAddress)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_nextcloud_notes_username),
                descriptionWhenEmpty = stringResource(R.string.pref_nextcloud_notes_username_hint),
            ).Render(
                value = data.username,
                onValueChange = { username ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setUsername(username)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_nextcloud_notes_password),
                descriptionWhenEmpty = stringResource(R.string.pref_nextcloud_notes_password_hint),
            ).Render(
                value = data.password,
                onValueChange = { password ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setPassword(password)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_nextcloud_notes_target_note),
                descriptionWhenEmpty = stringResource(R.string.pref_nextcloud_notes_target_note_hint),
            ).Render(
                value = data.targetNote,
                onValueChange = { targetNote ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setTargetNote(targetNote)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_nextcloud_notes_target_shopping_list),
                descriptionWhenEmpty = stringResource(R.string.pref_nextcloud_notes_target_shopping_list_hint),
            ).Render(
                value = data.targetShoppingList,
                onValueChange = { targetShoppingList ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setTargetShoppingList(targetShoppingList)
                                .build()
                        }
                    }
                },
            )
        }
    }
}

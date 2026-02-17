package org.stypox.dicio.skills.nextcloud_notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

sealed interface NextcloudNotesOutput : SkillOutput {
    data class Success(
        val noteName: String,
        val content: String,
        val isShoppingList: Boolean,
    ) : NextcloudNotesOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = if (isShoppingList) {
            ctx.getString(R.string.skill_nextcloud_notes_added_to_shopping_list, content)
        } else {
            ctx.getString(R.string.skill_nextcloud_notes_note_added, content)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Text(
                    text = if (isShoppingList) {
                        stringResource(R.string.skill_nextcloud_notes_added_to_shopping_list, content)
                    } else {
                        stringResource(R.string.skill_nextcloud_notes_note_added, content)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = androidx.compose.ui.Modifier.testTag("nextcloud_notes_success")
                )
                Text(
                    text = stringResource(R.string.skill_nextcloud_notes_saved_to, noteName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    data class QueryShoppingListSuccess(
        val noteName: String,
        val items: List<String>
    ) : NextcloudNotesOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            return if (items.isEmpty()) {
                ctx.getString(R.string.skill_nextcloud_notes_shopping_list_empty)
            } else {
                val itemsText = items.joinToString(", ")
                ctx.getString(R.string.skill_nextcloud_notes_shopping_list_items, items.size, itemsText)
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Text(
                    text = if (items.isEmpty()) {
                        stringResource(R.string.skill_nextcloud_notes_shopping_list_empty)
                    } else {
                        stringResource(R.string.skill_nextcloud_notes_shopping_list_num_of_items, items.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("nextcloud_notes_shopping_list_title")
                )
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    items.forEachIndexed { index, item ->
                        Text(
                            text = "- $item",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("nextcloud_notes_shopping_item_$index")
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.skill_nextcloud_notes_saved_to, noteName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    data class ItemFound(
        val item: String,
        val found: Boolean
    ) : NextcloudNotesOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            return if (found) {
                ctx.getString(R.string.skill_nextcloud_notes_item_found, item)
            } else {
                ctx.getString(R.string.skill_nextcloud_notes_item_not_found, item)
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Text(
                    text = if (found) {
                        stringResource(R.string.skill_nextcloud_notes_item_found, item)
                    } else {
                        stringResource(R.string.skill_nextcloud_notes_item_not_found, item)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("nextcloud_notes_item_check")
                )
            }
        }
    }

    data class Failed(
        val reason: FailureReason,
        val errorMessage: String? = null
    ) : NextcloudNotesOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = when (reason) {
            FailureReason.SETTINGS_MISSING -> ctx.getString(R.string.skill_nextcloud_notes_settings_missing)
            FailureReason.TARGET_NOTE_MISSING -> ctx.getString(R.string.skill_nextcloud_notes_target_note_missing)
            FailureReason.TARGET_SHOPPING_LIST_MISSING -> ctx.getString(R.string.skill_nextcloud_notes_target_shopping_list_missing)
            FailureReason.CONTENT_EMPTY -> ctx.getString(R.string.skill_nextcloud_notes_content_empty)
            FailureReason.CONNECTION_ERROR -> ctx.getString(
                R.string.skill_nextcloud_notes_connection_error,
                errorMessage ?: ctx.getString(R.string.skill_nextcloud_notes_unknown_error)
            )
        }
    }

    enum class FailureReason {
        SETTINGS_MISSING,
        TARGET_NOTE_MISSING,
        TARGET_SHOPPING_LIST_MISSING,
        CONTENT_EMPTY,
        CONNECTION_ERROR
    }
}

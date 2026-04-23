package org.stypox.dicio.skills.homeassistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.json.JSONObject
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

sealed interface HomeAssistantOutput : SkillOutput {
    data class GetStatusSuccess(
        val entityId: String,
        val friendlyName: String,
        val state: String,
        val attributes: JSONObject?
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_state,
            friendlyName,
            state
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_entity_state, "", state),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class SetStateSuccess(
        val entityId: String,
        val friendlyName: String,
        val action: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_set_success,
            friendlyName,
            action
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_set_success, "", action),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class EntityNotMapped(
        val entityName: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_not_mapped,
            entityName
        )
    }

    data class EntityNotFound(
        val entityId: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_entity_not_found,
            entityId
        )
    }

    data class InvalidAction(
        val action: String,
        val entityType: String
    ) : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_invalid_action,
            action,
            entityType
        )
    }

    class ConnectionFailed : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_connection_failed
        )
    }

    class AuthFailed : HomeAssistantOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_auth_failed
        )
    }

    data class SelectSourceSuccess(
        val entityId: String,
        val friendlyName: String,
        val sourceName: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_select_source_success,
            sourceName,
            friendlyName
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(
                        R.string.skill_homeassistant_select_source_success,
                        sourceName,
                        friendlyName
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class NoSourceList(
        val friendlyName: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_no_source_list,
            friendlyName
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(
                        R.string.skill_homeassistant_no_source_list,
                        ""
                    ).trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    data class SourceNotFound(
        val requestedSource: String,
        val friendlyName: String
    ) : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_source_not_found,
            requestedSource,
            friendlyName
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = friendlyName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_source_not_found_short),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "\"$requestedSource\"",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    class HelpResponse : HomeAssistantOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_homeassistant_help_speech
        )

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_help_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = ctx.getString(R.string.skill_homeassistant_help_content),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

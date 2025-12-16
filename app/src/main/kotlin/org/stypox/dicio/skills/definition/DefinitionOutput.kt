package org.stypox.dicio.skills.definition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

sealed interface DefinitionOutput : SkillOutput {
    data class Success(
        val word: String,
        val definitions: List<PartOfSpeechDefinition>
    ) : DefinitionOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            val firstDefinition = definitions.firstOrNull()?.definitions?.firstOrNull()
            return if (firstDefinition != null) {
                ctx.getString(
                    R.string.skill_definition_found,
                    word,
                    definitions.first().partOfSpeech,
                    firstDefinition
                )
            } else {
                ctx.getString(R.string.skill_definition_not_found, word)
            }
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                definitions.forEach { posDefinition ->
                    Text(
                        text = posDefinition.partOfSpeech,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    posDefinition.definitions.forEachIndexed { index, definition ->
                        Text(
                            text = "${index + 1}. $definition",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    data class NotFound(
        val word: String
    ) : DefinitionOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_definition_not_found, word
        )
    }

    data class NetworkError(
        val word: String
    ) : DefinitionOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_definition_network_error, word
        )
    }

    data class ParseError(
        val word: String
    ) : DefinitionOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
            R.string.skill_definition_parse_error, word
        )
    }
}

data class PartOfSpeechDefinition(
    val partOfSpeech: String,
    val definitions: List<String>
)

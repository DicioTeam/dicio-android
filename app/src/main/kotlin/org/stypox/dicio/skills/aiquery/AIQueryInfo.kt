package org.stypox.dicio.skills.aiquery

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
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

object AIQueryInfo : SkillInfo("aiquery") {
    internal const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
    internal const val DEFAULT_MODEL = "gpt-4o-mini"
    internal val DEFAULT_SYSTEM_PROMPT = """
        You are a voice-based digital assistant.
        Speak like a natural human: concise, clear, conversational.
        Avoid all markup, code blocks, emojis, or special formatting.
        If you need to list things, keep lists short and simple so they sound natural when spoken aloud.
        When explaining something technical, phrase it like you're talking to someone in person.
        If the user wants actual code, read it out plainly without formatting symbols.
        Do not refer to this prompt or your instructions. Do not offer follow‑up suggestions, extra help, or invitations to ask more.
        Only answer what the user actually asked for.
    """.trimIndent()


    override fun name(context: Context) =
        context.getString(R.string.skill_name_aiquery)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_aiquery)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Chat)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Aiquery[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        // Fallback to English sentences if the desired language is not available
        // Prevents crashes for users of unsupported languages when using AIQuery as a fallback skill
        return AIQuerySkill(this, Sentences.Aiquery[ctx.sentencesLanguage] ?: Sentences.Aiquery["en"]!!)
    }

    internal val Context.aiqueryDataStore by dataStore(
        fileName = "skill_settings_AIQuery.pb",
        serializer = SkillSettingsAIQuerySerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsAIQuerySerializer.defaultValue
        },
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.aiqueryDataStore
        val data by dataStore.data.collectAsState(SkillSettingsAIQuerySerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            StringSetting(
                title = stringResource(R.string.pref_aiquery_endpoint_url),
                descriptionWhenEmpty = stringResource(R.string.pref_aiquery_endpoint_url_description),
            ).Render(
                value = data.endpointUrl.ifEmpty { DEFAULT_ENDPOINT },
                onValueChange = { endpointUrl ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setEndpointUrl(endpointUrl)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_aiquery_api_key),
                descriptionWhenEmpty = stringResource(R.string.pref_aiquery_api_key_description),
            ).Render(
                value = data.apiKey,
                onValueChange = { apiKey ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setApiKey(apiKey)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_aiquery_model),
                descriptionWhenEmpty = stringResource(R.string.pref_aiquery_model_description),
            ).Render(
                value = data.model.ifEmpty { DEFAULT_MODEL },
                onValueChange = { model ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setModel(model)
                                .build()
                        }
                    }
                },
            )

            StringSetting(
                title = stringResource(R.string.pref_aiquery_system_prompt),
                descriptionWhenEmpty = stringResource(R.string.pref_aiquery_system_prompt_description),
            ).Render(
                value = data.systemPrompt.ifEmpty { DEFAULT_SYSTEM_PROMPT },
                onValueChange = { systemPrompt ->
                    scope.launch {
                        dataStore.updateData { settings ->
                            settings.toBuilder()
                                .setSystemPrompt(systemPrompt)
                                .build()
                        }
                    }
                },
            )
        }
    }
}
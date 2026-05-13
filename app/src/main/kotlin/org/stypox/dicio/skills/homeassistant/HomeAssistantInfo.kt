package org.stypox.dicio.skills.homeassistant

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.ui.StringSetting

private const val TAG = "HomeAssistantInfo"

object HomeAssistantInfo : SkillInfo("home_assistant") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_home_assistant)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_home_assistant)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Home)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.HomeAssistant[ctx.sentencesLanguage] ?: return null
        return HomeAssistantSkill(HomeAssistantInfo, data)
    }

    internal val Context.homeAssistantDataStore by dataStore(
        fileName = "skill_settings_home_assistant.pb",
        serializer = SkillSettingsHomeAssistantSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsHomeAssistantSerializer.defaultValue
        }
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val context = LocalContext.current
        val dataStore = context.homeAssistantDataStore
        val data by dataStore.data.collectAsState(SkillSettingsHomeAssistantSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            StringSetting(
                title = stringResource(R.string.pref_homeassistant_base_url),
            ).Render(
                value = data.baseUrl,
                onValueChange = { baseUrl ->
                    Log.d(TAG, "Saving base URL: $baseUrl")
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            dataStore.updateData {
                                Log.d(TAG, "DataStore update started")
                                it.toBuilder().setBaseUrl(baseUrl).build()
                            }
                            Log.d(TAG, "DataStore update completed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save base URL", e)
                        }
                    }
                }
            )

            StringSetting(
                title = stringResource(R.string.pref_homeassistant_access_token),
            ).Render(
                value = data.accessToken,
                onValueChange = { token ->
                    Log.d(TAG, "Saving access token (length: ${token.length})")
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            dataStore.updateData {
                                Log.d(TAG, "DataStore update started")
                                it.toBuilder().setAccessToken(token).build()
                            }
                            Log.d(TAG, "DataStore update completed")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save access token", e)
                        }
                    }
                }
            )

            EntityMappingsEditor(
                mappings = data.entityMappingsList,
                baseUrl = data.baseUrl,
                accessToken = data.accessToken,
                onMappingsChange = { mappings ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().clearEntityMappings().addAllEntityMappings(mappings).build()
                        }
                    }
                },
            )
        }
    }
}

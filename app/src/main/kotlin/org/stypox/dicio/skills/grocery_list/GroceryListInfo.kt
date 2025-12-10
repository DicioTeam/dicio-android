package org.stypox.dicio.skills.grocery_list

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
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

object GroceryListInfo : SkillInfo("grocery_list") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_grocery_list)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_grocery_list)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.ShoppingCart)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.GroceryList[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return GroceryListSkill(GroceryListInfo, Sentences.GroceryList[ctx.sentencesLanguage]!!)
    }

    internal val Context.groceryListDataStore by dataStore(
        fileName = "skill_settings_grocery_list.pb",
        serializer = SkillSettingsGroceryListSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsGroceryListSerializer.defaultValue
        },
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.groceryListDataStore
        val data by dataStore.data.collectAsState(SkillSettingsGroceryListSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            StringSetting(
                title = stringResource(R.string.pref_grocery_list_endpoint_url),
                descriptionWhenEmpty = stringResource(R.string.pref_grocery_list_endpoint_url_description),
            ).Render(
                value = data.endpointUrl,
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
                title = stringResource(R.string.pref_grocery_list_api_key),
                descriptionWhenEmpty = stringResource(R.string.pref_grocery_list_api_key_description),
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
        }
    }
}
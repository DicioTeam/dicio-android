package org.stypox.dicio.skills.homeassistant

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import kotlinx.coroutines.launch
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.ui.StringSetting

object HomeAssistantInfo : SkillInfo("home_assistant") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_home_assistant)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_home_assistant)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Home)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.HomeAssistant[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant[ctx.sentencesLanguage]!!)
    }

    internal val Context.homeAssistantDataStore by dataStore(
        fileName = "skill_settings_home_assistant.pb",
        serializer = SkillSettingsHomeAssistantSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            SkillSettingsHomeAssistantSerializer.defaultValue
        }
    )

    override val renderSettings: @Composable () -> Unit get() = @Composable {
        val dataStore = LocalContext.current.homeAssistantDataStore
        val data by dataStore.data.collectAsState(SkillSettingsHomeAssistantSerializer.defaultValue)
        val scope = rememberCoroutineScope()

        Column {
            StringSetting(
                title = stringResource(R.string.pref_homeassistant_base_url),
            ).Render(
                value = data.baseUrl,
                onValueChange = { baseUrl ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().setBaseUrl(baseUrl).build()
                        }
                    }
                }
            )

            StringSetting(
                title = stringResource(R.string.pref_homeassistant_access_token),
            ).Render(
                value = data.accessToken,
                onValueChange = { token ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().setAccessToken(token).build()
                        }
                    }
                }
            )

            EntityMappingsEditor(
                mappings = data.entityMappingsList,
                onMappingsChange = { mappings ->
                    scope.launch {
                        dataStore.updateData {
                            it.toBuilder().clearEntityMappings().addAllEntityMappings(mappings).build()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun EntityMappingsEditor(
    mappings: List<EntityMapping>,
    onMappingsChange: (List<EntityMapping>) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editIndex by rememberSaveable { mutableStateOf(-1) }

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pref_homeassistant_entity_mappings),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = {
                    editIndex = -1
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.pref_homeassistant_add_mapping))
                }
            }
        }

        itemsIndexed(mappings) { index, mapping ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        editIndex = index
                        showDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mapping.friendlyName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = mapping.entityId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onMappingsChange(mappings.filterIndexed { i, _ -> i != index })
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    if (showDialog) {
        EntityMappingDialog(
            initialMapping = if (editIndex >= 0) mappings[editIndex] else null,
            onDismiss = { showDialog = false },
            onSave = { friendlyName, entityId ->
                val newMapping = EntityMapping.newBuilder()
                    .setFriendlyName(friendlyName)
                    .setEntityId(entityId)
                    .build()
                
                val newMappings = if (editIndex >= 0) {
                    mappings.toMutableList().apply { set(editIndex, newMapping) }
                } else {
                    mappings + newMapping
                }
                onMappingsChange(newMappings)
                showDialog = false
            }
        )
    }
}

@Composable
fun EntityMappingDialog(
    initialMapping: EntityMapping?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var friendlyName by rememberSaveable { mutableStateOf(initialMapping?.friendlyName ?: "") }
    var entityId by rememberSaveable { mutableStateOf(initialMapping?.entityId ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (initialMapping == null)
                        stringResource(R.string.pref_homeassistant_add_mapping)
                    else
                        "Edit Mapping",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                TextField(
                    value = friendlyName,
                    onValueChange = { friendlyName = it },
                    label = { Text(stringResource(R.string.pref_homeassistant_friendly_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                TextField(
                    value = entityId,
                    onValueChange = { entityId = it },
                    label = { Text(stringResource(R.string.pref_homeassistant_entity_id)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = { onSave(friendlyName, entityId) },
                        enabled = friendlyName.isNotBlank() && entityId.isNotBlank()
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

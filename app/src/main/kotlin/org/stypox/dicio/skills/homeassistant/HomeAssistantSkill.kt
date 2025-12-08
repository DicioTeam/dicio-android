package org.stypox.dicio.skills.homeassistant

import kotlinx.coroutines.flow.first
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.HomeAssistant
import org.stypox.dicio.skills.homeassistant.HomeAssistantInfo.homeAssistantDataStore
import java.io.FileNotFoundException

class HomeAssistantSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<HomeAssistant>
) : StandardRecognizerSkill<HomeAssistant>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: HomeAssistant): SkillOutput {
        android.util.Log.d("HomeAssistantSkill", "generateOutput called with inputData: $inputData")
        val settings = ctx.android.homeAssistantDataStore.data.first()
        
        val (entityName, action) = when (inputData) {
            is HomeAssistant.GetStatus -> Pair(inputData.entityName ?: "", null)
            is HomeAssistant.SetStateOn -> Pair(inputData.entityName ?: "", "on")
            is HomeAssistant.SetStateOff -> Pair(inputData.entityName ?: "", "off")
            is HomeAssistant.SetStateToggle -> Pair(inputData.entityName ?: "", "toggle")
        }
        
        val mapping = findBestMatch(entityName, settings.entityMappingsList)
            ?: return HomeAssistantOutput.EntityNotMapped(entityName)
        
        return try {
            when (inputData) {
                is HomeAssistant.GetStatus -> handleGetStatus(settings, mapping)
                is HomeAssistant.SetStateOn, is HomeAssistant.SetStateOff, is HomeAssistant.SetStateToggle -> 
                    handleSetState(settings, mapping, action!!)
            }
        } catch (e: FileNotFoundException) {
            HomeAssistantOutput.EntityNotFound(mapping.entityId)
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                HomeAssistantOutput.AuthFailed()
            } else {
                HomeAssistantOutput.ConnectionFailed()
            }
        }
    }

    private suspend fun handleGetStatus(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping
    ): SkillOutput {
        val state = HomeAssistantApi.getEntityState(
            settings.baseUrl,
            settings.accessToken,
            mapping.entityId
        )
        
        return HomeAssistantOutput.GetStatusSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            state = state.getString("state"),
            attributes = state.optJSONObject("attributes")
        )
    }

    private suspend fun handleSetState(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping,
        action: String
    ): SkillOutput {
        android.util.Log.d("HomeAssistantSkill", "handleSetState - action: '$action', entityId: '${mapping.entityId}'")
        val domain = mapping.entityId.substringBefore(".")
        android.util.Log.d("HomeAssistantSkill", "Domain: '$domain'")
        val parsedAction = parseAction(action, domain)
        if (parsedAction == null) {
            android.util.Log.e("HomeAssistantSkill", "Failed to parse action: '$action'")
            return HomeAssistantOutput.InvalidAction(action.ifEmpty { "<empty>" }, domain)
        }
        android.util.Log.d("HomeAssistantSkill", "Parsed action: service='${parsedAction.service}', spokenForm='${parsedAction.spokenForm}'")
        
        val service = when (domain) {
            "cover" -> when (parsedAction.service) {
                "turn_on" -> "open_cover"
                "turn_off" -> "close_cover"
                else -> parsedAction.service
            }
            "lock" -> when (parsedAction.service) {
                "turn_on" -> "unlock"
                "turn_off" -> "lock"
                else -> parsedAction.service
            }
            else -> parsedAction.service
        }
        
        HomeAssistantApi.callService(
            settings.baseUrl,
            settings.accessToken,
            domain,
            service,
            mapping.entityId
        )
        
        return HomeAssistantOutput.SetStateSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            action = parsedAction.spokenForm
        )
    }

    private fun findBestMatch(spokenName: String, mappings: List<EntityMapping>): EntityMapping? {
        val normalized = spokenName.lowercase().replace(Regex("\\b(the|a|an)\\b"), "").trim()
        
        mappings.firstOrNull { it.friendlyName.lowercase() == normalized }?.let { return it }
        
        return mappings.firstOrNull {
            it.friendlyName.lowercase().contains(normalized) ||
            normalized.contains(it.friendlyName.lowercase())
        }
    }

    private data class ParsedAction(val service: String, val spokenForm: String)

    private fun parseAction(action: String, domain: String): ParsedAction? {
        val normalized = action.lowercase().trim()
        android.util.Log.d("HomeAssistantSkill", "parseAction - input: '$action', normalized: '$normalized'")
        
        return when {
            normalized.contains("on") || normalized in listOf("open", "unlock", "enable") ->
                ParsedAction("turn_on", "on")
            normalized.contains("off") || normalized in listOf("close", "lock", "disable") ->
                ParsedAction("turn_off", "off")
            normalized.contains("toggle") ->
                ParsedAction("toggle", "toggled")
            else -> null
        }
    }
}

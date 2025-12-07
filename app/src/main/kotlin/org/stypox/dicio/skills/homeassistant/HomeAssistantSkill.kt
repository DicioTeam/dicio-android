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
        val settings = ctx.android.homeAssistantDataStore.data.first()
        
        val entityName = when (inputData) {
            is HomeAssistant.GetStatus -> inputData.entityName ?: ""
            is HomeAssistant.SetState -> inputData.entityName ?: ""
        }
        
        val mapping = findBestMatch(entityName, settings.entityMappingsList)
            ?: return HomeAssistantOutput.EntityNotMapped(entityName)
        
        return try {
            when (inputData) {
                is HomeAssistant.GetStatus -> handleGetStatus(settings, mapping)
                is HomeAssistant.SetState -> handleSetState(settings, mapping, inputData.action ?: "")
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
        val domain = mapping.entityId.substringBefore(".")
        val parsedAction = parseAction(action, domain)
            ?: return HomeAssistantOutput.InvalidAction(action, domain)
        
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
        
        return when {
            normalized in listOf("on", "open", "unlock", "enable") ->
                ParsedAction("turn_on", normalized)
            normalized in listOf("off", "close", "lock", "disable") ->
                ParsedAction("turn_off", normalized)
            normalized.startsWith("toggle") ->
                ParsedAction("toggle", "toggled")
            else -> null
        }
    }
}

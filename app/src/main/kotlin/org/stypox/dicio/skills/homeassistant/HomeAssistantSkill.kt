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
        
        return try {
            when (inputData) {
                is HomeAssistant.GetHelp -> {
                    handleGetHelp()
                }
                is HomeAssistant.GetStatus -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleGetStatus(settings, mapping)
                }
                is HomeAssistant.GetPersonLocation -> {
                    val personName = inputData.personName?.trim() ?: ""
                    val mapping = findBestMatch(personName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(personName)
                    handleGetStatus(settings, mapping)
                }
                is HomeAssistant.SetStateOn -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "on")
                }
                is HomeAssistant.SetStateOff -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "off")
                }
                is HomeAssistant.SetStateToggle -> {
                    val entityName = inputData.entityName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSetState(settings, mapping, "toggle")
                }
                is HomeAssistant.SelectSource -> {
                    val entityName = inputData.entityName ?: ""
                    val sourceName = inputData.sourceName ?: ""
                    val mapping = findBestMatch(entityName, settings.entityMappingsList)
                        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
                    handleSelectSource(settings, mapping, sourceName)
                }
            }
        } catch (e: FileNotFoundException) {
            HomeAssistantOutput.EntityNotFound("unknown")
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                HomeAssistantOutput.AuthFailed()
            } else {
                HomeAssistantOutput.ConnectionFailed()
            }
        }
    }

    private fun handleGetHelp(): SkillOutput {
        return HomeAssistantOutput.HelpResponse()
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

    private suspend fun handleSelectSource(
        settings: SkillSettingsHomeAssistant,
        mapping: EntityMapping,
        requestedSource: String
    ): SkillOutput {
        // Get entity state to retrieve source_list
        val state = HomeAssistantApi.getEntityState(
            settings.baseUrl,
            settings.accessToken,
            mapping.entityId
        )
        
        // Extract source_list attribute
        val attributes = state.optJSONObject("attributes")
        val sourceListJson = attributes?.optJSONArray("source_list")
        
        if (sourceListJson == null || sourceListJson.length() == 0) {
            return HomeAssistantOutput.NoSourceList(mapping.friendlyName)
        }
        
        // Convert to list
        val sourceList = (0 until sourceListJson.length())
            .map { sourceListJson.getString(it) }
        
        // Fuzzy match requested source
        val matchedSource = findBestSourceMatch(requestedSource, sourceList)
            ?: return HomeAssistantOutput.SourceNotFound(
                requestedSource,
                mapping.friendlyName
            )
        
        // Call select_source service
        HomeAssistantApi.callService(
            settings.baseUrl,
            settings.accessToken,
            "media_player",
            "select_source",
            mapping.entityId,
            mapOf("source" to matchedSource)
        )
        
        return HomeAssistantOutput.SelectSourceSuccess(
            entityId = mapping.entityId,
            friendlyName = mapping.friendlyName,
            sourceName = matchedSource
        )
    }

    private fun generateNumberVariations(input: String): List<String> {
        // Map number words to their digit and homophone variations
        val numberMappings = mapOf(
            "one" to listOf("1", "won"),
            "two" to listOf("2", "to", "too"),
            "three" to listOf("3"),
            "four" to listOf("4", "for", "fore"),
            "five" to listOf("5"),
            "six" to listOf("6"),
            "seven" to listOf("7"),
            "eight" to listOf("8", "ate"),
            "nine" to listOf("9"),
            "ten" to listOf("10")
        )
        
        // Build reverse map: homophone -> (number word, all variations)
        val reverseMap = mutableMapOf<String, Pair<String, List<String>>>()
        for ((word, variations) in numberMappings) {
            reverseMap[word] = word to (listOf(word) + variations)
            for (variation in variations) {
                reverseMap[variation] = word to (listOf(word) + variations)
            }
        }
        
        val variations = mutableListOf(input)
        
        // Find all number words or homophones in input
        for ((trigger, pair) in reverseMap) {
            val regex = Regex("\\b$trigger\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(input)) {
                val (_, allVariations) = pair
                for (replacement in allVariations) {
                    if (replacement.lowercase() != trigger.lowercase()) {
                        variations.add(regex.replace(input, replacement))
                    }
                }
            }
        }
        
        return variations.distinct()
    }

    private fun findBestSourceMatch(requested: String, available: List<String>): String? {
        val variations = generateNumberVariations(requested)
        
        // 1. Try exact match with each variation
        for (variation in variations) {
            val normalized = variation.lowercase().trim()
            available.firstOrNull { it.lowercase() == normalized }?.let { return it }
        }
        
        // 2. Fuzzy match with all variations (skip contains - too greedy for short words)
        val allMatches = variations.flatMap { variation ->
            val normalized = variation.lowercase().trim()
            available.mapIndexed { index, source ->
                Triple(source, calculateSimilarity(normalized, source.lowercase()), index)
            }
        }
        
        val scored = allMatches.filter { it.second >= 0.4 }
        
        // Prefer higher similarity, then shorter match, then earlier in list
        return scored.maxWithOrNull(
            compareBy({ it.second }, { -it.first.length }, { -it.third })
        )?.first
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.split(Regex("\\s+")).toSet()
        val words2 = s2.split(Regex("\\s+")).toSet()
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return if (union > 0) intersection.toDouble() / union else 0.0
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

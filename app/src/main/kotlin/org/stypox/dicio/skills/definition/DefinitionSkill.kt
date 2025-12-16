package org.stypox.dicio.skills.definition

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.stypox.dicio.sentences.Sentences.Definition
import org.stypox.dicio.util.ConnectionUtils
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

class DefinitionSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Definition>) :
    StandardRecognizerSkill<Definition>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Definition): SkillOutput {
        val word: String = when (inputData) {
            is Definition.Query -> inputData.word ?: return DefinitionOutput.NotFound(word = "")
        }

        // Get language code from locale (e.g., "en", "fr", "de")
        val languageCode = ctx.locale.language.lowercase(Locale.getDefault())
        
        // Build Wiktionary API URL based on user's locale
        val apiUrl = "https://$languageCode.wiktionary.org/api/rest_v1/page/definition/" +
                ConnectionUtils.percentEncode(word.trim())

        return try {
            val definitionData = ConnectionUtils.getPageJson(apiUrl)
            parseDefinitions(word, definitionData)
        } catch (e: FileNotFoundException) {
            // 404 - word not found in Wiktionary
            DefinitionOutput.NotFound(word = word)
        } catch (e: IOException) {
            // Network error
            DefinitionOutput.NetworkError(word = word)
        } catch (e: JSONException) {
            // Failed to parse response
            DefinitionOutput.ParseError(word = word)
        }
    }

    private fun parseDefinitions(word: String, data: JSONObject): SkillOutput {
        try {
            // Wiktionary API returns language-specific definitions
            // The structure is: { "en": [ { "partOfSpeech": "...", "definitions": [...] }, ... ] }
            // or sometimes just an array at the root level
            
            val languageKeys = data.keys()
            if (!languageKeys.hasNext()) {
                return DefinitionOutput.NotFound(word = word)
            }

            // Get the first language's definitions (usually matches the Wiktionary language)
            val firstLanguageKey = languageKeys.next()
            val definitionsArray: JSONArray = data.getJSONArray(firstLanguageKey)

            if (definitionsArray.length() == 0) {
                return DefinitionOutput.NotFound(word = word)
            }

            val posDefinitions = mutableListOf<PartOfSpeechDefinition>()

            // Parse each part of speech
            for (i in 0 until definitionsArray.length()) {
                val posObject = definitionsArray.getJSONObject(i)
                val partOfSpeech = posObject.optString("partOfSpeech", "Unknown")
                val defsArray = posObject.optJSONArray("definitions")

                if (defsArray != null && defsArray.length() > 0) {
                    val definitions = mutableListOf<String>()
                    
                    // Only take the first 3 definitions per part of speech
                    val maxDefinitions = minOf(3, defsArray.length())
                    for (j in 0 until maxDefinitions) {
                        val defObject = defsArray.getJSONObject(j)
                        val definition = defObject.optString("definition", "")
                        if (definition.isNotEmpty()) {
                            // Clean up the definition text (remove HTML tags if any)
                            definitions.add(cleanDefinitionText(definition))
                        }
                    }

                    if (definitions.isNotEmpty()) {
                        posDefinitions.add(
                            PartOfSpeechDefinition(
                                partOfSpeech = partOfSpeech,
                                definitions = definitions
                            )
                        )
                    }
                }
            }

            return if (posDefinitions.isNotEmpty()) {
                DefinitionOutput.Success(word = word, definitions = posDefinitions)
            } else {
                DefinitionOutput.NotFound(word = word)
            }
        } catch (e: JSONException) {
            return DefinitionOutput.ParseError(word = word)
        }
    }

    private fun cleanDefinitionText(text: String): String {
        // Remove HTML tags and extra whitespace
        return text
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
}

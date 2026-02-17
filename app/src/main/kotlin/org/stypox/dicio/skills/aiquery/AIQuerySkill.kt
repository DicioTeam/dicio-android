package org.stypox.dicio.skills.aiquery

import kotlinx.coroutines.flow.first
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Aiquery
import org.stypox.dicio.skills.aiquery.AIQueryInfo.aiqueryDataStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIQuerySkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Aiquery>
) : StandardRecognizerSkill<Aiquery>(correspondingSkillInfo, data) {
    
    private val client = OkHttpClient()
    
    override suspend fun generateOutput(ctx: SkillContext, inputData: Aiquery): SkillOutput {
        val prefs = ctx.android.aiqueryDataStore.data.first()
        
        val question = when (inputData) {
            is Aiquery.Query -> inputData.question
        } ?: return AIQueryOutput(null, "")

        if (prefs.apiKey.isEmpty()) {
            return AIQueryOutput(null, question) // Error: no API key configured
        }

        return try {
            val response = callAIQueryApi(
                question = question,
                apiKey = prefs.apiKey,
                model = prefs.model.ifEmpty { AIQueryInfo.DEFAULT_MODEL },
                systemPrompt = prefs.systemPrompt.ifEmpty { AIQueryInfo.DEFAULT_SYSTEM_PROMPT },
                endpointUrl = prefs.endpointUrl.ifEmpty { AIQueryInfo.DEFAULT_ENDPOINT }
            )
            
            AIQueryOutput(response, question)
        } catch (e: Exception) {
            AIQueryOutput(null, question)
        }
    }

    private suspend fun callAIQueryApi(
        question: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        endpointUrl: String
    ): String = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                })
            })
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API call failed: ${response.code}")
            }
            
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response")
            
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
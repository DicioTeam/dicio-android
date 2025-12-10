package org.stypox.dicio.skills.grocery_list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.json.JSONObject
import org.stypox.dicio.sentences.Sentences.GroceryList
import org.stypox.dicio.skills.grocery_list.GroceryListInfo.groceryListDataStore

class GroceryListSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<GroceryList>
) : StandardRecognizerSkill<GroceryList>(correspondingSkillInfo, data) {

    private val client = OkHttpClient()

    override suspend fun generateOutput(ctx: SkillContext, inputData: GroceryList): SkillOutput {
        val prefs = ctx.android.groceryListDataStore.data.first()
        
        return when (inputData) {
            is GroceryList.Add -> {
                val item = inputData.item ?: return GroceryListOutput("", false, false)
                
                // Check if endpoint is configured
                if (prefs.endpointUrl.isEmpty()) {
                    return GroceryListOutput(item, false, true)
                }
                
                addItem(
                    ctx,
                    item,
                    prefs.endpointUrl,
                    prefs.apiKey
                )
            }
        }
    }

    private suspend fun addItem(
        ctx: SkillContext,
        item: String,
        endpointUrl: String,
        apiKey: String
    ): SkillOutput {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("action", "add_item")
                    put("name", item)
                    if (apiKey.isNotEmpty()) {
                        put("api_key", apiKey)
                    }
                }

                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)

                if (apiKey.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext GroceryListOutput(item, false, false)
                    }

                    val responseBody = response.body?.string()
                        ?: return@withContext GroceryListOutput(item, false, false)
                    
                    val responseJson = JSONObject(responseBody)
                    val success = responseJson.optBoolean("success", false)

                    GroceryListOutput(item, success, false)
                }
            } catch (e: Exception) {
                GroceryListOutput(item, false, false)
            }
        }
    }
}
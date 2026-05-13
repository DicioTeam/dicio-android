package org.stypox.dicio.skills.homeassistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object HomeAssistantApi {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    @Throws(IOException::class)
    suspend fun getAllStates(baseUrl: String, token: String): JSONArray {
        val body = executeRequest(
            Request.Builder()
                .url("$baseUrl/api/states")
                .addHeader("Authorization", "Bearer $token")
                .build()
        )
        return JSONArray(body)
    }

    @Throws(IOException::class)
    suspend fun getEntityState(baseUrl: String, token: String, entityId: String): JSONObject {
        val body = executeRequest(
            Request.Builder()
                .url("$baseUrl/api/states/$entityId")
                .addHeader("Authorization", "Bearer $token")
                .build()
        )
        return JSONObject(body)
    }

    @Throws(IOException::class)
    suspend fun callService(
        baseUrl: String,
        token: String,
        domain: String,
        service: String,
        entityId: String,
        extraParams: Map<String, String> = emptyMap()
    ): JSONArray {
        val jsonBody = JSONObject().put("entity_id", entityId)
        extraParams.forEach { (key, value) -> jsonBody.put(key, value) }

        val body = executeRequest(
            Request.Builder()
                .url("$baseUrl/api/services/$domain/$service")
                .addHeader("Authorization", "Bearer $token")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        )
        return JSONArray(body)
    }

    private fun executeRequest(request: Request): String {
        val response = client.newCall(request).execute()
        return response.use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code}: ${it.message}")
            it.body?.string() ?: throw IOException("Empty response body")
        }
    }
}

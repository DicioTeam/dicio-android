package org.stypox.dicio.skills.homeassistant

import org.json.JSONArray
import org.json.JSONObject
import org.stypox.dicio.util.ConnectionUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HomeAssistantApi {
    @Throws(IOException::class)
    suspend fun getAllStates(baseUrl: String, token: String): JSONArray {
        val connection = URL("$baseUrl/api/states").openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }

    @Throws(IOException::class)
    suspend fun getEntityState(baseUrl: String, token: String, entityId: String): JSONObject {
        val connection = URL("$baseUrl/api/states/$entityId").openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONObject(response)
    }

    @Throws(IOException::class)
    suspend fun callService(
        baseUrl: String,
        token: String,
        domain: String,
        service: String,
        entityId: String
    ): JSONArray {
        val connection = URL("$baseUrl/api/services/$domain/$service").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val body = JSONObject().put("entity_id", entityId).toString()
        connection.outputStream.write(body.toByteArray())
        
        val scanner = java.util.Scanner(connection.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()
        
        return JSONArray(response)
    }
}

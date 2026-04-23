package org.stypox.dicio.skills.homeassistant

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.OutputStream

object HomeAssistantYamlUtils {
    
    data class YamlEntityMapping(
        val friendlyName: String,
        val entityId: String
    )
    
    data class YamlHomeAssistantConfig(
        val baseUrl: String = "",
        val accessToken: String = "",
        val entityMappings: List<YamlEntityMapping> = emptyList()
    )
    
    fun exportToYaml(
        baseUrl: String,
        accessToken: String,
        mappings: List<EntityMapping>,
        outputStream: OutputStream
    ) {
        val config = YamlHomeAssistantConfig(
            baseUrl = baseUrl,
            accessToken = accessToken,
            entityMappings = mappings.map { 
                YamlEntityMapping(it.friendlyName, it.entityId) 
            }
        )
        
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        
        val yaml = Yaml(options)
        outputStream.writer().use { writer ->
            yaml.dump(mapOf("homeAssistant" to config), writer)
        }
    }
    
    fun importFromYaml(inputStream: InputStream): YamlHomeAssistantConfig {
        val yaml = Yaml()
        val data = yaml.load<Map<String, Any>>(inputStream)
        val haConfig = data["homeAssistant"] as? Map<String, Any> ?: return YamlHomeAssistantConfig()
        
        val baseUrl = haConfig["baseUrl"] as? String ?: ""
        val accessToken = haConfig["accessToken"] as? String ?: ""
        val mappingsData = haConfig["entityMappings"] as? List<Map<String, Any>> ?: emptyList()
        
        val mappings = mappingsData.mapNotNull { mapping ->
            val friendlyName = mapping["friendlyName"] as? String
            val entityId = mapping["entityId"] as? String
            if (friendlyName != null && entityId != null) {
                YamlEntityMapping(friendlyName, entityId)
            } else null
        }
        
        return YamlHomeAssistantConfig(baseUrl, accessToken, mappings)
    }
}

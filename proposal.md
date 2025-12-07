# Home Assistant Skill Proposal

## Overview
Add a new skill to Dicio that allows users to query and control Home Assistant entities via voice commands:
- Query: "Get home assistant status for [entity name]"
- Control: "Set home assistant [entity name] on/off" or "Turn [entity name] on/off"

## Challenge: Entity Name Mapping
The main challenge is mapping user-friendly names (e.g., "living room light") to Home Assistant entity IDs (e.g., "light.living_room_main"). This requires a user-configurable mapping system.

## Implementation Plan

### 1. Sentence Definitions

**File: `app/src/main/sentences/skill_definitions.yml`**
```yaml
- id: home_assistant
  specificity: high
  sentences:
    - id: get_status
      captures:
        - id: entity_name
          type: string
    - id: set_state
      captures:
        - id: entity_name
          type: string
        - id: action
          type: string
```

**File: `app/src/main/sentences/en/home_assistant.yml`**
```yaml
get_status:
  - (get|what is|whats|check) (the )?(home assistant |ha )?status (of|for) .entity_name.
  - (get|what is|whats|check) .entity_name. (status|state)
  - how is .entity_name. doing
  - is .entity_name. on|off

set_state:
  - (set|turn|switch) (the )?(home assistant |ha )?.entity_name. .action.
  - .action. (the )?.entity_name.
```

### 2. Proto Definition for Settings

**File: `app/src/main/proto/skill_settings_home_assistant.proto`**
```proto
syntax = "proto3";

option java_package = "org.stypox.dicio.skills.homeassistant";
option java_multiple_files = true;

message SkillSettingsHomeAssistant {
  string base_url = 1;  // e.g., "http://192.168.1.100:8123"
  string access_token = 2;  // Long-Lived Access Token
  repeated EntityMapping entity_mappings = 3;
}

message EntityMapping {
  string friendly_name = 1;  // e.g., "living room light"
  string entity_id = 2;      // e.g., "light.living_room_main"
}
```

### 3. Package Structure

**Create: `org.stypox.dicio.skills.homeassistant`**

Files needed:
- `HomeAssistantInfo.kt` - Skill metadata and settings UI
- `HomeAssistantSkill.kt` - Main skill logic
- `HomeAssistantOutput.kt` - Output rendering (speech + UI)
- `HomeAssistantApi.kt` - API client for Home Assistant REST API
- `SkillSettingsHomeAssistantSerializer.kt` - Proto serializer

### 4. Key Implementation Details

#### HomeAssistantInfo.kt
- Provide settings UI with:
  - `StringSetting` for base URL
  - `StringSetting` for access token (consider masking)
  - Custom Composable for entity mappings list (add/edit/delete)
- Check availability: URL and token must be configured

#### HomeAssistantSkill.kt
- Match user input `entity_name` against configured friendly names (fuzzy matching)
- For `get_status`: Call `GET /api/states/{entity_id}`
- For `set_state`: 
  - Parse action (on/off/open/close/lock/unlock, etc.)
  - Determine service from entity domain (e.g., `light.turn_on`, `switch.turn_off`)
  - Call `POST /api/services/{domain}/{service}` with `{"entity_id": "..."}`
- Handle errors: entity not found, connection failed, auth failed, invalid action

#### HomeAssistantOutput.kt
- Sealed interface with:
  - `GetStatusSuccess(entityId, friendlyName, state, attributes)` - Show state and relevant attributes
  - `SetStateSuccess(entityId, friendlyName, action, newState)` - Confirm action completed
  - `EntityNotMapped(entityName)` - "I don't know which entity you mean"
  - `EntityNotFound(entityId)` - "Entity not found in Home Assistant"
  - `InvalidAction(action, entityType)` - "Cannot perform [action] on [entity type]"
  - `ConnectionFailed()` - "Could not connect to Home Assistant"
  - `AuthFailed()` - "Authentication failed"

#### HomeAssistantApi.kt
```kotlin
suspend fun getEntityState(baseUrl: String, token: String, entityId: String): JSONObject {
    return ConnectionUtils.getPageJson(
        url = "$baseUrl/api/states/$entityId",
        headers = mapOf("Authorization" to "Bearer $token")
    )
}

suspend fun callService(baseUrl: String, token: String, domain: String, service: String, entityId: String): JSONArray {
    return ConnectionUtils.postPageJson(
        url = "$baseUrl/api/services/$domain/$service",
        headers = mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        ),
        body = JSONObject().put("entity_id", entityId)
    )
}
```

### 5. Entity Mapping UI Design

The settings should include a list of entity mappings with:
- Display: "friendly name → entity_id"
- Add button: Opens dialog with two text fields
- Edit: Tap item to edit
- Delete: Swipe or long-press to delete

Implementation approach:
```kotlin
@Composable
fun EntityMappingsEditor(
    mappings: List<EntityMapping>,
    onMappingsChange: (List<EntityMapping>) -> Unit
)
```

### 6. Register Skill

**File: `app/src/main/kotlin/org/stypox/dicio/eval/SkillHandler.kt`**

Add to `allSkillInfoList`:
```kotlin
HomeAssistantInfo,
```

### 7. String Resources

**File: `app/src/main/res/values/strings.xml`**
```xml
<string name="skill_name_home_assistant">Home Assistant</string>
<string name="skill_sentence_example_home_assistant">Turn living room light on</string>
<string name="skill_homeassistant_entity_state">%1$s is %2$s</string>
<string name="skill_homeassistant_set_success">%1$s turned %2$s</string>
<string name="skill_homeassistant_invalid_action">Cannot %1$s a %2$s</string>
<string name="skill_homeassistant_entity_not_mapped">I don\'t have a mapping for %1$s</string>
<string name="skill_homeassistant_entity_not_found">Entity %1$s not found in Home Assistant</string>
<string name="skill_homeassistant_connection_failed">Could not connect to Home Assistant</string>
<string name="skill_homeassistant_auth_failed">Home Assistant authentication failed</string>
<string name="pref_homeassistant_base_url">Home Assistant URL</string>
<string name="pref_homeassistant_access_token">Access Token</string>
<string name="pref_homeassistant_entity_mappings">Entity Mappings</string>
<string name="pref_homeassistant_add_mapping">Add Mapping</string>
<string name="pref_homeassistant_friendly_name">Friendly Name</string>
<string name="pref_homeassistant_entity_id">Entity ID</string>
```

### 8. Fuzzy Matching Strategy

Since users may say entity names slightly differently:
```kotlin
fun findBestMatch(spokenName: String, mappings: List<EntityMapping>): EntityMapping? {
    // Normalize: lowercase, remove articles (the, a, an)
    val normalized = spokenName.lowercase().replace(Regex("\\b(the|a|an)\\b"), "").trim()
    
    // Exact match first
    mappings.firstOrNull { it.friendlyName.lowercase() == normalized }?.let { return it }
    
    // Contains match
    return mappings.firstOrNull { 
        it.friendlyName.lowercase().contains(normalized) || 
        normalized.contains(it.friendlyName.lowercase())
    }
}
```

### 9. Security Considerations

- Access token should be stored securely (DataStore with encryption if possible)
- Warn users about storing tokens in plain text
- Support HTTPS URLs
- Validate URL format before saving

### 10. Action Parsing Logic

Map user actions to Home Assistant services:

```kotlin
data class ParsedAction(val service: String, val spokenForm: String)

fun parseAction(action: String, domain: String): ParsedAction? {
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
```

Domain-specific service mapping:
- `light.*` → `light.turn_on`, `light.turn_off`, `light.toggle`
- `switch.*` → `switch.turn_on`, `switch.turn_off`, `switch.toggle`
- `cover.*` → `cover.open_cover`, `cover.close_cover`
- `lock.*` → `lock.lock`, `lock.unlock`
- `climate.*` → `climate.turn_on`, `climate.turn_off`

### 11. Future Enhancements

- Set specific values (brightness, temperature, etc.)
- Support for multiple Home Assistant instances
- Auto-discovery of entities via API
- Import entity list from Home Assistant
- Support for scenes and automations
- Voice confirmation before executing actions (optional setting)

## Testing Checklist

- [ ] Sentence recognition works for various phrasings (get and set)
- [ ] Settings UI allows adding/editing/deleting mappings
- [ ] API calls work with valid credentials
- [ ] GET requests retrieve entity states correctly
- [ ] POST requests control entities correctly
- [ ] Error handling for all failure cases
- [ ] Fuzzy matching finds entities correctly
- [ ] Action parsing handles on/off/toggle/open/close/lock/unlock
- [ ] Domain-specific service mapping works correctly
- [ ] Speech output is natural and informative
- [ ] Graphical output displays state clearly
- [ ] Works with different entity types (lights, switches, covers, locks, sensors, etc.)
- [ ] Invalid actions are rejected with helpful messages

## Documentation Updates

- Add to README.md under Skills section
- Add to fastlane/metadata/android/en-US/full_description.txt
- Create example in .amazonq/rules/homeassistant.md (already exists)

## Estimated Complexity

**Medium-High** due to:
- Custom settings UI for entity mappings (most complex part)
- Network API integration
- Error handling for multiple failure modes
- Fuzzy matching logic

The weather skill provides a good template, but the entity mapping UI is more complex than a simple string setting.

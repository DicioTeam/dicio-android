# Home Assistant Media Source Control - Design

## Architecture Overview

This feature extends the existing Home Assistant skill to support media player source selection through a two-stage matching process:
1. Match user command pattern and extract entity/source names
2. Fetch available sources from the media player and fuzzy match the requested source

## Component Design

### 1. Sentence Recognition

**Location:** `app/src/main/sentences/skill_definitions.yml`

Add new sentence type to `home_assistant` skill:

```yaml
- id: select_source
  captures:
    - id: entity_name
      type: string
    - id: source_name
      type: string
```

**Location:** `app/src/main/sentences/en/home_assistant.yml`

Add sentence patterns:

```yaml
select_source:
  - (turn|switch|set|tune|change) (the )?.entity_name. to .source_name.
  - (tune|set) (the )?.entity_name. on .source_name.
```

### 2. Data Flow

```
User Input
    ↓
Sentence Recognition (StandardRecognizerSkill)
    ↓
HomeAssistant.SelectSource(entityName, sourceName)
    ↓
HomeAssistantSkill.generateOutput()
    ↓
1. Find entity mapping (fuzzy match entityName)
    ↓
2. Fetch entity state from HA API
    ↓
3. Extract source_list attribute
    ↓
4. Fuzzy match sourceName against source_list
    ↓
5. Call media_player.select_source service
    ↓
HomeAssistantOutput.SelectSourceSuccess/Failed
    ↓
UI + Speech Output
```

### 3. Code Changes

#### 3.1 HomeAssistantSkill.kt

Add new `when` branch in `generateOutput()`:

```kotlin
is HomeAssistant.SelectSource -> {
    val entityName = inputData.entityName ?: ""
    val sourceName = inputData.sourceName ?: ""
    val mapping = findBestMatch(entityName, settings.entityMappingsList)
        ?: return HomeAssistantOutput.EntityNotMapped(entityName)
    handleSelectSource(settings, mapping, sourceName)
}
```

Add new handler method:

```kotlin
private suspend fun handleSelectSource(
    settings: SkillSettingsHomeAssistant,
    mapping: EntityMapping,
    requestedSource: String
): SkillOutput {
    // 1. Get entity state to retrieve source_list
    val state = HomeAssistantApi.getEntityState(
        settings.baseUrl,
        settings.accessToken,
        mapping.entityId
    )
    
    // 2. Extract source_list attribute
    val attributes = state.optJSONObject("attributes")
    val sourceListJson = attributes?.optJSONArray("source_list")
    
    if (sourceListJson == null || sourceListJson.length() == 0) {
        return HomeAssistantOutput.NoSourceList(mapping.friendlyName)
    }
    
    // 3. Convert to list
    val sourceList = (0 until sourceListJson.length())
        .map { sourceListJson.getString(it) }
    
    // 4. Fuzzy match requested source
    val matchedSource = findBestSourceMatch(requestedSource, sourceList)
        ?: return HomeAssistantOutput.SourceNotFound(
            requestedSource, 
            mapping.friendlyName
        )
    
    // 5. Call select_source service
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
```

Add fuzzy matching helper:

```kotlin
private fun findBestSourceMatch(requested: String, available: List<String>): String? {
    val normalized = requested.lowercase().trim()
    
    // Exact match
    available.firstOrNull { it.lowercase() == normalized }?.let { return it }
    
    // Contains match
    available.firstOrNull { 
        it.lowercase().contains(normalized) || 
        normalized.contains(it.lowercase())
    }?.let { return it }
    
    // Fuzzy match with scoring
    val scored = available.map { source ->
        source to calculateSimilarity(normalized, source.lowercase())
    }.filter { it.second > 0.6 } // Threshold
    
    return scored.maxByOrNull { it.second }?.first
}

private fun calculateSimilarity(s1: String, s2: String): Double {
    // Simple word-based similarity
    val words1 = s1.split(Regex("\\s+")).toSet()
    val words2 = s2.split(Regex("\\s+")).toSet()
    val intersection = words1.intersect(words2).size
    val union = words1.union(words2).size
    return if (union > 0) intersection.toDouble() / union else 0.0
}
```

#### 3.2 HomeAssistantApi.kt

Extend existing `callService()` method to support additional parameters:

```kotlin
@Throws(IOException::class)
suspend fun callService(
    baseUrl: String,
    token: String,
    domain: String,
    service: String,
    entityId: String,
    extraParams: Map<String, String> = emptyMap()
): JSONArray {
    val connection = URL("$baseUrl/api/services/$domain/$service")
        .openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true
    
    val body = JSONObject().put("entity_id", entityId)
    extraParams.forEach { (key, value) -> body.put(key, value) }
    
    connection.outputStream.write(body.toString().toByteArray())
    
    val scanner = java.util.Scanner(connection.inputStream)
    val response = scanner.useDelimiter("\\A").next()
    scanner.close()
    
    return JSONArray(response)
}
```

**Note:** This extends the existing method signature with an optional `extraParams` parameter, maintaining backward compatibility.

#### 3.3 HomeAssistantOutput.kt

Add new output types:

```kotlin
data class SelectSourceSuccess(
    val entityId: String,
    val friendlyName: String,
    val sourceName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_select_source_success,
            sourceName,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        // Display success message with entity and source
    }
}

data class NoSourceList(
    val friendlyName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_no_source_list,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        // Display error message
    }
}

data class SourceNotFound(
    val requestedSource: String,
    val friendlyName: String
) : HomeAssistantOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(
            R.string.skill_home_assistant_source_not_found,
            requestedSource,
            friendlyName
        )
    
    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        // Display error message with requested source
    }
}
```

### 4. String Resources

**Location:** `app/src/main/res/values/strings.xml`

Add new strings:

```xml
<string name="skill_home_assistant_select_source_success">Playing %1$s on %2$s</string>
<string name="skill_home_assistant_no_source_list">%1$s does not have any available sources</string>
<string name="skill_home_assistant_source_not_found">Could not find source %1$s on %2$s</string>
```

## Sequence Diagram

```
User -> Dicio: "turn kitchen radio to BBC Radio 2"
Dicio -> StandardRecognizer: Match sentence pattern
StandardRecognizer -> HomeAssistantSkill: SelectSource(entityName="kitchen radio", sourceName="BBC Radio 2")
HomeAssistantSkill -> HomeAssistantSkill: findBestMatch("kitchen radio", mappings)
HomeAssistantSkill -> HomeAssistantApi: getEntityState(baseUrl, token, "media_player.kitchen_radio")
HomeAssistantApi -> HomeAssistant: GET /api/states/media_player.kitchen_radio
HomeAssistant -> HomeAssistantApi: {state: "playing", attributes: {source_list: ["BBC Radio 2", "BBC Radio 4", ...]}}
HomeAssistantApi -> HomeAssistantSkill: JSONObject
HomeAssistantSkill -> HomeAssistantSkill: Extract source_list
HomeAssistantSkill -> HomeAssistantSkill: findBestSourceMatch("BBC Radio 2", ["BBC Radio 2", "BBC Radio 4", ...])
HomeAssistantSkill -> HomeAssistantApi: callService(baseUrl, token, "media_player", "select_source", entityId, {"source": "BBC Radio 2"})
HomeAssistantApi -> HomeAssistant: POST /api/services/media_player/select_source {entity_id: "...", source: "BBC Radio 2"}
HomeAssistant -> HomeAssistantApi: Success response
HomeAssistantApi -> HomeAssistantSkill: JSONArray
HomeAssistantSkill -> Dicio: SelectSourceSuccess(friendlyName="kitchen radio", sourceName="BBC Radio 2")
Dicio -> User: "Playing BBC Radio 2 on kitchen radio"
```

## Error Handling

### Error Scenarios

1. **Entity not mapped**
   - User says entity name not in mappings
   - Return: `HomeAssistantOutput.EntityNotMapped`
   - Speech: "I don't know about [entity name]"

2. **Entity not found in HA**
   - Mapped entity doesn't exist in Home Assistant
   - Catch: `FileNotFoundException` from API
   - Return: `HomeAssistantOutput.EntityNotFound`
   - Speech: "Could not find [entity name]"

3. **No source_list attribute**
   - Entity exists but has no source_list (not a media player or doesn't support sources)
   - Return: `HomeAssistantOutput.NoSourceList`
   - Speech: "[entity name] does not have any available sources"

4. **Source not matched**
   - Requested source doesn't fuzzy match any available source
   - Return: `HomeAssistantOutput.SourceNotFound`
   - Speech: "Could not find source [source name] on [entity name]"

5. **API call failure**
   - Network error, auth error, or service call failure
   - Catch: `IOException` or HTTP error codes
   - Return: `HomeAssistantOutput.ConnectionFailed` or `HomeAssistantOutput.AuthFailed`
   - Speech: "Failed to connect to Home Assistant" or "Authentication failed"

## Fuzzy Matching Strategy

### Algorithm

The fuzzy matching uses a three-tier approach:

1. **Exact match** (case-insensitive)
   - Direct string comparison after normalization
   - Example: "BBC Radio 2" matches "BBC Radio 2"

2. **Contains match**
   - Check if either string contains the other
   - Example: "Radio 2" matches "BBC Radio 2"

3. **Word-based similarity**
   - Calculate Jaccard similarity of word sets
   - Threshold: 0.6 (60% similarity required)
   - Example: "BBC Radio Two" matches "BBC Radio 2" (words: {bbc, radio, two/2})

### Normalization

- Convert to lowercase
- Trim whitespace
- Remove articles (the, a, an) for entity names only
- Keep source names intact (articles may be significant)

## Testing Considerations

### Unit Tests

1. **Fuzzy matching tests**
   - Exact matches
   - Partial matches
   - Word-based matches
   - No match scenarios
   - Edge cases (empty strings, special characters)

2. **Source list extraction**
   - Valid source_list
   - Empty source_list
   - Missing source_list attribute
   - Null attributes object

3. **Error handling**
   - Entity not mapped
   - No source_list
   - Source not found
   - API failures

### Integration Tests

1. Mock Home Assistant API responses
2. Test full flow from sentence to output
3. Test all error scenarios
4. Test with various entity and source names

## Implementation Considerations

### Reuse Existing Infrastructure

- Use existing `findBestMatch()` for entity name matching
- Follow existing error handling patterns
- Use existing API connection methods
- Follow existing output structure

### Minimal Changes

- Add one new sentence type
- Add one new handler method
- Extend existing `callService()` API method with optional parameters
- Add three new output types
- Add three new string resources

### Backward Compatibility

- No changes to existing sentence types
- No changes to existing API methods
- No changes to existing output types
- All existing tests should pass

## Performance

- **API calls:** 2 per request (getEntityState + callSelectSource)
- **Fuzzy matching:** O(n) where n = number of sources (typically < 20)
- **Response time:** < 2 seconds typical (network dependent)

## Future Enhancements (Out of Scope)

- Cache source_list to reduce API calls
- Support for media_content_id playback
- Volume control integration
- Playlist management
- Multi-room audio
- Custom fuzzy matching threshold configuration

# Home Assistant Media Source Control - Design

## Architecture Overview

This feature extends the existing Home Assistant skill to support media player source selection through a two-stage matching process:
1. Match user command pattern and extract entity/source names
2. Fetch available sources from the media player and fuzzy match the requested source

## Related Documentation

- **[API Research](./api-research.md)** - Home Assistant media_player API documentation and validation
- **[Fuzzy Matching Tests](./fuzzy-match-tests.md)** - Test cases with real source data from kitchen_radio_2
- **[Entity Mapping Configuration](./entity-mapping-config.md)** - Entity mapping setup and requirements
- **[UI Design](./ui-design.md)** - Jetpack Compose UI specifications for outputs
- **[Unknowns Summary](./unknowns-summary.md)** - Investigation findings and resolutions

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
    val variations = generateNumberVariations(requested)
    
    // Try each variation and score against all available sources
    val allMatches = variations.flatMap { variation ->
        val normalized = variation.lowercase().trim()
        
        // Exact match (highest priority)
        available.firstOrNull { it.lowercase() == normalized }?.let { 
            return it 
        }
        
        // Contains match (medium priority)
        available.firstOrNull { 
            it.lowercase().contains(normalized) || 
            normalized.contains(it.lowercase())
        }?.let { 
            return it 
        }
        
        // Fuzzy match with scoring (lowest priority)
        available.map { source ->
            Triple(source, variation, calculateSimilarity(normalized, source.lowercase()))
        }
    }
    
    // Return best fuzzy match above threshold
    return allMatches
        .filter { it.third > 0.5 }
        .maxByOrNull { it.third }
        ?.first
}

private fun generateNumberVariations(input: String): List<String> {
    val numberWords = mapOf(
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
    
    val variations = mutableListOf(input)
    
    for ((word, replacements) in numberWords) {
        val regex = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(input)) {
            for (replacement in replacements) {
                variations.add(regex.replace(input, replacement))
            }
        }
    }
    
    return variations.distinct()
}

private fun calculateSimilarity(s1: String, s2: String): Double {
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

### Fuzzy Matching Strategy

See **[fuzzy-match-tests.md](./fuzzy-match-tests.md)** for comprehensive test cases with real source data.

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

See **[fuzzy-match-tests.md](./fuzzy-match-tests.md)** for detailed test cases based on real source data from kitchen_radio_2.

### Unit Tests

#### 1. Fuzzy Matching Tests (`HomeAssistantSkillTest.kt`)

**Test Class:** `SelectSourceFuzzyMatchingTest`

```kotlin
@Test
fun `exact match - case insensitive`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("bbc radio 2", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `partial match - user says shortened name`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("radio 2", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `fuzzy match - number as word`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("BBC Radio Two", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `fuzzy match - phonetic spelling`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("bee bee see radio 2", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `fuzzy match - missing words`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("radio two", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `fuzzy match - extra words`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("the BBC Radio 2 station", available)
    assertEquals("BBC Radio 2", result)
}

@Test
fun `no match - completely different`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("Netflix", available)
    assertNull(result)
}

@Test
fun `no match - below threshold`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "Spotify")
    val result = findBestSourceMatch("xyz", available)
    assertNull(result)
}

@Test
fun `ambiguous match - returns best score`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4", "BBC Radio 6 Music")
    val result = findBestSourceMatch("BBC Radio", available)
    assertNotNull(result)
    assertTrue(result in available)
}

@Test
fun `empty source list`() {
    val available = emptyList<String>()
    val result = findBestSourceMatch("BBC Radio 2", available)
    assertNull(result)
}

@Test
fun `empty requested source`() {
    val available = listOf("BBC Radio 2", "BBC Radio 4")
    val result = findBestSourceMatch("", available)
    assertNull(result)
}

@Test
fun `special characters in source name`() {
    val available = listOf("80's Hits", "90's Rock", "Today's Top 40")
    val result = findBestSourceMatch("80s hits", available)
    assertEquals("80's Hits", result)
}

@Test
fun `unicode characters in source name`() {
    val available = listOf("Café del Mar", "Música Latina", "Rock")
    val result = findBestSourceMatch("cafe del mar", available)
    assertEquals("Café del Mar", result)
}
```

#### 2. Source List Extraction Tests

```kotlin
@Test
fun `extract source_list from valid state`() {
    val state = JSONObject("""
        {
            "entity_id": "media_player.kitchen_radio",
            "state": "playing",
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4", "Spotify"]
            }
        }
    """)
    
    val sourceList = extractSourceList(state)
    assertEquals(3, sourceList.size)
    assertTrue(sourceList.contains("BBC Radio 2"))
}

@Test
fun `extract source_list - empty array`() {
    val state = JSONObject("""
        {
            "entity_id": "media_player.kitchen_radio",
            "state": "playing",
            "attributes": {
                "source_list": []
            }
        }
    """)
    
    val sourceList = extractSourceList(state)
    assertTrue(sourceList.isEmpty())
}

@Test
fun `extract source_list - missing attribute`() {
    val state = JSONObject("""
        {
            "entity_id": "media_player.kitchen_radio",
            "state": "playing",
            "attributes": {}
        }
    """)
    
    val sourceList = extractSourceList(state)
    assertNull(sourceList)
}

@Test
fun `extract source_list - null attributes`() {
    val state = JSONObject("""
        {
            "entity_id": "media_player.kitchen_radio",
            "state": "playing"
        }
    """)
    
    val sourceList = extractSourceList(state)
    assertNull(sourceList)
}
```

#### 3. Error Handling Tests

```kotlin
@Test
fun `handleSelectSource - entity not mapped`() = runTest {
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "unknown device",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.EntityNotMapped)
    assertEquals("unknown device", (result as HomeAssistantOutput.EntityNotMapped).entityName)
}

@Test
fun `handleSelectSource - no source list`() = runTest {
    // Mock API to return state without source_list
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.NoSourceList)
}

@Test
fun `handleSelectSource - source not found`() = runTest {
    // Mock API to return state with source_list that doesn't match
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "Netflix"
    ))
    
    assertTrue(result is HomeAssistantOutput.SourceNotFound)
    assertEquals("Netflix", (result as HomeAssistantOutput.SourceNotFound).requestedSource)
}

@Test
fun `handleSelectSource - API connection failure`() = runTest {
    // Mock API to throw IOException
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.ConnectionFailed)
}

@Test
fun `handleSelectSource - authentication failure`() = runTest {
    // Mock API to throw 401 error
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.AuthFailed)
}
```

#### 4. Similarity Calculation Tests

```kotlin
@Test
fun `calculateSimilarity - identical strings`() {
    val similarity = calculateSimilarity("bbc radio 2", "bbc radio 2")
    assertEquals(1.0, similarity, 0.01)
}

@Test
fun `calculateSimilarity - no common words`() {
    val similarity = calculateSimilarity("bbc radio 2", "spotify")
    assertEquals(0.0, similarity, 0.01)
}

@Test
fun `calculateSimilarity - partial overlap`() {
    val similarity = calculateSimilarity("bbc radio 2", "bbc radio 4")
    assertTrue(similarity > 0.5)
}

@Test
fun `calculateSimilarity - subset`() {
    val similarity = calculateSimilarity("radio 2", "bbc radio 2")
    assertTrue(similarity > 0.6)
}
```

### Integration Tests

#### 1. End-to-End Flow Tests (`HomeAssistantSkillIntegrationTest.kt`)

**Test Class:** `SelectSourceIntegrationTest`

```kotlin
@Test
fun `full flow - exact match success`() = runTest {
    // Mock Home Assistant API responses
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "entity_id": "media_player.kitchen_radio",
            "state": "playing",
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4", "Spotify"]
            }
        }
    """)
    
    mockApi.mockCallService("media_player", "select_source", success = true)
    
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.SelectSourceSuccess)
    val success = result as HomeAssistantOutput.SelectSourceSuccess
    assertEquals("BBC Radio 2", success.sourceName)
    assertEquals("kitchen radio", success.friendlyName)
    
    // Verify API was called with correct parameters
    mockApi.verifyServiceCalled("media_player", "select_source", mapOf(
        "entity_id" to "media_player.kitchen_radio",
        "source" to "BBC Radio 2"
    ))
}

@Test
fun `full flow - fuzzy match success`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4", "Spotify"]
            }
        }
    """)
    
    mockApi.mockCallService("media_player", "select_source", success = true)
    
    // User says "Radio Two" but source is "BBC Radio 2"
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "Radio Two"
    ))
    
    assertTrue(result is HomeAssistantOutput.SelectSourceSuccess)
    val success = result as HomeAssistantOutput.SelectSourceSuccess
    assertEquals("BBC Radio 2", success.sourceName) // Matched to exact source
    
    // Verify correct source was sent to API
    mockApi.verifyServiceCalled("media_player", "select_source", mapOf(
        "source" to "BBC Radio 2"
    ))
}

@Test
fun `full flow - phonetic match success`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4"]
            }
        }
    """)
    
    mockApi.mockCallService("media_player", "select_source", success = true)
    
    // User says phonetic spelling
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "bee bee see radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.SelectSourceSuccess)
    assertEquals("BBC Radio 2", (result as HomeAssistantOutput.SelectSourceSuccess).sourceName)
}

@Test
fun `full flow - partial match success`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4", "Spotify"]
            }
        }
    """)
    
    mockApi.mockCallService("media_player", "select_source", success = true)
    
    // User says shortened name
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.SelectSourceSuccess)
    assertEquals("BBC Radio 2", (result as HomeAssistantOutput.SelectSourceSuccess).sourceName)
}

@Test
fun `full flow - no match failure`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {
                "source_list": ["BBC Radio 2", "BBC Radio 4", "Spotify"]
            }
        }
    """)
    
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "Netflix"
    ))
    
    assertTrue(result is HomeAssistantOutput.SourceNotFound)
    val error = result as HomeAssistantOutput.SourceNotFound
    assertEquals("Netflix", error.requestedSource)
    assertEquals("kitchen radio", error.friendlyName)
    
    // Verify service was NOT called
    mockApi.verifyServiceNotCalled("media_player", "select_source")
}

@Test
fun `full flow - empty source list`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {
                "source_list": []
            }
        }
    """)
    
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.NoSourceList)
}

@Test
fun `full flow - missing source list attribute`() = runTest {
    mockApi.mockGetEntityState("media_player.kitchen_radio", """
        {
            "attributes": {}
        }
    """)
    
    val result = skill.generateOutput(ctx, HomeAssistant.SelectSource(
        entityName = "kitchen radio",
        sourceName = "BBC Radio 2"
    ))
    
    assertTrue(result is HomeAssistantOutput.NoSourceList)
}
```

#### 2. Sentence Recognition Tests

```kotlin
@Test
fun `sentence recognition - turn to pattern`() {
    val input = "turn kitchen radio to BBC Radio 2"
    val result = recognizer.recognize(input)
    
    assertTrue(result is HomeAssistant.SelectSource)
    val selectSource = result as HomeAssistant.SelectSource
    assertEquals("kitchen radio", selectSource.entityName)
    assertEquals("BBC Radio 2", selectSource.sourceName)
}

@Test
fun `sentence recognition - set on pattern`() {
    val input = "set kitchen radio on BBC Radio 2"
    val result = recognizer.recognize(input)
    
    assertTrue(result is HomeAssistant.SelectSource)
    val selectSource = result as HomeAssistant.SelectSource
    assertEquals("kitchen radio", selectSource.entityName)
    assertEquals("BBC Radio 2", selectSource.sourceName)
}

@Test
fun `sentence recognition - with the article`() {
    val input = "turn the kitchen radio to BBC Radio 2"
    val result = recognizer.recognize(input)
    
    assertTrue(result is HomeAssistant.SelectSource)
    assertEquals("kitchen radio", (result as HomeAssistant.SelectSource).entityName)
}

@Test
fun `sentence recognition - does not conflict with set_state_on`() {
    val input = "turn kitchen radio on"
    val result = recognizer.recognize(input)
    
    // Should match set_state_on, not select_source
    assertTrue(result is HomeAssistant.SetStateOn)
}
```

### Test Coverage Goals

- **Fuzzy matching:** 100% coverage of matching algorithm
- **Error handling:** All error scenarios covered
- **API integration:** All API calls mocked and verified
- **Sentence recognition:** All patterns tested
- **Edge cases:** Empty strings, special characters, unicode

### Mock Setup

```kotlin
class MockHomeAssistantApi {
    private val entityStates = mutableMapOf<String, String>()
    private val serviceCalls = mutableListOf<ServiceCall>()
    
    fun mockGetEntityState(entityId: String, jsonResponse: String) {
        entityStates[entityId] = jsonResponse
    }
    
    fun mockCallService(domain: String, service: String, success: Boolean) {
        // Mock implementation
    }
    
    fun verifyServiceCalled(domain: String, service: String, params: Map<String, String>) {
        val call = serviceCalls.find { 
            it.domain == domain && it.service == service 
        }
        assertNotNull(call)
        assertEquals(params, call.params)
    }
    
    fun verifyServiceNotCalled(domain: String, service: String) {
        val call = serviceCalls.find { 
            it.domain == domain && it.service == service 
        }
        assertNull(call)
    }
}
```

## Entity Mapping Configuration

See **[entity-mapping-config.md](./entity-mapping-config.md)** for complete configuration requirements and user setup instructions.

The select source feature reuses the existing entity mapping infrastructure. No additional configuration is needed beyond the standard entity mappings already used by other Home Assistant skill features.

## UI Design

See **[ui-design.md](./ui-design.md)** for complete Jetpack Compose UI specifications.

All output types follow the existing Home Assistant skill UI patterns with simple Column layouts displaying entity name as headline and confirmation/error messages as body text.

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

### Detailed Test Specifications

See comprehensive test specifications above including:
- 15+ fuzzy matching test cases covering exact, partial, phonetic, and edge cases
- Source list extraction tests for all scenarios
- Error handling tests for all failure modes
- End-to-end integration tests with mocked API
- Sentence recognition tests to verify no conflicts with existing patterns
- Mock setup for API testing

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
- **Fuzzy matching:** O(n*m) where n = number of sources, m = number of variations (typically < 20 * 5)
- **Response time:** < 2 seconds typical (network dependent)

## Unit Testing Strategy

### Number Variation Generation Tests

Test `generateNumberVariations()` with various inputs:

1. **Single number word**: "BBC Radio two" → ["BBC Radio two", "BBC Radio 2", "BBC Radio to", "BBC Radio too"]
2. **Multiple number words**: "one two three" → all combinations
3. **No number words**: "BBC Radio" → ["BBC Radio"]
4. **Number at start**: "two BBC Radio" → variations
5. **Number at end**: "BBC Radio two" → variations
6. **Mixed case**: "BBC Radio Two" → case-insensitive variations
7. **All number words**: one through ten
8. **Homophones**: "two" → ["2", "to", "too"], "four" → ["4", "for", "fore"], "eight" → ["8", "ate"]

### Fuzzy Matching with Variations Tests

Test `findBestSourceMatch()` with speech-to-text scenarios:

1. **Exact match with digit**: "BBC Radio 2" finds "BBC Radio 2"
2. **Homophone "too"**: "BBC Radio too" finds "BBC Radio 2" (not "BBC Radio 4")
3. **Homophone "to"**: "BBC Radio to" finds "BBC Radio 2"
4. **Homophone "for"**: "BBC Radio for" finds "BBC Radio 4"
5. **Homophone "ate"**: "Radio ate" finds "Radio 8"
6. **Multiple matches**: Prefer higher similarity score
7. **Tie-breaking**: When scores equal, prefer shorter match
8. **Below threshold**: "xyz" finds nothing (< 0.5 similarity)
9. **Partial match**: "Radio 2" finds "BBC Radio 2"
10. **Case insensitive**: "bbc radio 2" finds "BBC Radio 2"

### Integration Tests

Test end-to-end scenarios:

1. **Success with digit**: "turn kitchen radio to BBC Radio 2" → SelectSourceSuccess
2. **Success with homophone**: "turn kitchen radio to BBC Radio too" → SelectSourceSuccess (finds "BBC Radio 2")
3. **Entity not found**: "turn nonexistent to BBC Radio 2" → EntityNotFound
4. **No source list**: Entity with no sources → NoSourceList
5. **Source not found**: "turn kitchen radio to xyz" → SourceNotFound
6. **Service call failure**: Mock API failure → ServiceCallFailed

### Edge Cases

1. **Empty source list**: [] → NoSourceList
2. **Empty requested source**: "" → SourceNotFound
3. **Whitespace only**: "   " → SourceNotFound
4. **Special characters**: "BBC Radio 2 (UK)" → matches correctly
5. **Very long source name**: 100+ characters → handles gracefully
6. **Unicode characters**: "Rádio 2" → matches correctly

## Future Enhancements (Out of Scope)

- Cache source_list to reduce API calls
- Support for media_content_id playback
- Volume control integration
- Playlist management
- Multi-room audio
- Custom fuzzy matching threshold configuration

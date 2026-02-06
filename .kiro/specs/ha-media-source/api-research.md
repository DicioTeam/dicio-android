# Home Assistant Media Player API Research

## Service: media_player.select_source

### Official Documentation

**Source:** [Home Assistant Media Player Documentation](https://www.home-assistant.io/components/media_player)

### Service Call Format

```yaml
service: media_player.select_source
data:
  entity_id: media_player.kitchen_radio
  source: "BBC Radio 2"
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `entity_id` | No (defaults to all) | Target media player entity |
| `source` | Yes | Name of the source to switch to (platform dependent) |

### REST API Endpoint

```
POST /api/services/media_player/select_source
```

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Body:**
```json
{
  "entity_id": "media_player.kitchen_radio",
  "source": "BBC Radio 2"
}
```

**Response:**
```json
[
  {
    "entity_id": "media_player.kitchen_radio",
    "state": "playing"
  }
]
```

## Entity State and Attributes

### Getting Entity State

```
GET /api/states/media_player.kitchen_radio
```

**Response:**
```json
{
  "entity_id": "media_player.kitchen_radio",
  "state": "playing",
  "attributes": {
    "friendly_name": "Kitchen Radio",
    "source": "BBC Radio 2",
    "source_list": [
      "BBC Radio 2",
      "BBC Radio 4",
      "BBC Radio 6 Music",
      "Spotify"
    ],
    "volume_level": 0.5,
    "is_volume_muted": false,
    "supported_features": 152461
  },
  "last_changed": "2026-02-06T10:00:00.000000+00:00",
  "last_updated": "2026-02-06T10:00:00.000000+00:00"
}
```

### Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `source` | string | Currently selected source |
| `source_list` | array of strings | Available sources for this media player |
| `supported_features` | integer | Bitmask of supported features |

### Source List Attribute

The `source_list` attribute contains an array of available source names that can be passed to the `select_source` service.

**Important Notes:**
- The `source_list` attribute is **optional** - not all media players support it
- If a media player doesn't support source selection, this attribute will be `null` or missing
- The source names are **platform-dependent** and **case-sensitive**
- The exact source name from `source_list` must be used in the `select_source` call

**Example source_list values:**
- Sonos: `["TV", "Line-In", "Spotify", "Radio"]`
- Yamaha Receiver: `["HDMI1", "HDMI2", "Bluetooth", "Spotify"]`
- Universal Media Player: Custom defined sources

## Implementation Verification

### Our Design Matches the API

✅ **Correct approach:**
1. Get entity state: `GET /api/states/{entity_id}`
2. Extract `source_list` from `attributes`
3. Fuzzy match user's requested source against `source_list`
4. Call service: `POST /api/services/media_player/select_source` with exact matched source

### API Call Sequence

```
1. User: "turn kitchen radio to BBC Radio 2"
   ↓
2. GET /api/states/media_player.kitchen_radio
   Response: { attributes: { source_list: ["BBC Radio 2", "BBC Radio 4", ...] } }
   ↓
3. Fuzzy match "BBC Radio 2" → "BBC Radio 2" (exact match)
   ↓
4. POST /api/services/media_player/select_source
   Body: { entity_id: "media_player.kitchen_radio", source: "BBC Radio 2" }
   ↓
5. Success response
```

## Existing Implementation in HomeAssistantApi.kt

### Current Methods

```kotlin
// Already exists - can be used to get source_list
suspend fun getEntityState(baseUrl: String, token: String, entityId: String): JSONObject

// Already exists - generic service call
suspend fun callService(
    baseUrl: String,
    token: String,
    domain: String,
    service: String,
    entityId: String
): JSONArray
```

### New Method Needed

We need a specialized method for `select_source` that accepts the `source` parameter:

```kotlin
suspend fun callSelectSource(
    baseUrl: String,
    token: String,
    entityId: String,
    source: String
): JSONArray {
    val connection = URL("$baseUrl/api/services/media_player/select_source")
        .openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true
    
    val body = JSONObject()
        .put("entity_id", entityId)
        .put("source", source)
        .toString()
    connection.outputStream.write(body.toByteArray())
    
    val scanner = java.util.Scanner(connection.inputStream)
    val response = scanner.useDelimiter("\\A").next()
    scanner.close()
    
    return JSONArray(response)
}
```

## Design Validation

### ✅ Confirmed Design Decisions

1. **Two-stage process is correct:**
   - Stage 1: Get entity state to retrieve `source_list`
   - Stage 2: Match and call `select_source` service

2. **Fuzzy matching is necessary:**
   - Source names are platform-dependent
   - Speech-to-text may not be 100% accurate
   - User may not know exact source name

3. **Error handling scenarios are correct:**
   - Entity not found (404 from API)
   - No `source_list` attribute (media player doesn't support sources)
   - Source not matched (fuzzy match fails)
   - Service call failure (network/auth errors)

4. **API methods needed:**
   - ✅ `getEntityState()` - already exists
   - ⚠️ `callSelectSource()` - needs to be added (or use generic `callService()` with extra params)

### Alternative: Use Existing callService()

Instead of adding `callSelectSource()`, we could extend the existing `callService()` to accept optional parameters:

```kotlin
suspend fun callService(
    baseUrl: String,
    token: String,
    domain: String,
    service: String,
    entityId: String,
    extraParams: Map<String, String> = emptyMap()
): JSONArray {
    // ... existing code ...
    
    val body = JSONObject()
        .put("entity_id", entityId)
    
    // Add extra parameters
    extraParams.forEach { (key, value) ->
        body.put(key, value)
    }
    
    // ... rest of existing code ...
}
```

Then call it like:
```kotlin
HomeAssistantApi.callService(
    settings.baseUrl,
    settings.accessToken,
    "media_player",
    "select_source",
    mapping.entityId,
    mapOf("source" to matchedSource)
)
```

## Recommendation

**Use the existing `callService()` method with an extension** to support extra parameters. This is more flexible and avoids code duplication.

Update the design document to reflect this approach.

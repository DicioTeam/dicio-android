# Entity Mapping Configuration Requirements

## Overview

The Home Assistant skill requires entity mappings to translate user-friendly names (e.g., "kitchen radio") into Home Assistant entity IDs (e.g., "media_player.kitchen_radio_2").

## Entity Mapping Structure

### Data Model

```kotlin
data class EntityMapping(
    val friendlyName: String,  // User-friendly name: "kitchen radio"
    val entityId: String       // Home Assistant entity ID: "media_player.kitchen_radio_2"
)
```

### YAML Format

```yaml
homeAssistant:
  baseUrl: "http://homeassistant.local:8123"
  accessToken: "your_long_lived_access_token"
  entityMappings:
    - friendlyName: "kitchen radio"
      entityId: "media_player.kitchen_radio_2"
    - friendlyName: "living room light"
      entityId: "light.living_room"
    - friendlyName: "front door"
      entityId: "lock.front_door"
```

## Configuration Methods

### 1. Manual Configuration (Current)

Users manually add entity mappings through the Dicio app settings:
1. Open Dicio app
2. Navigate to Settings → Home Assistant
3. Add entity mappings one by one
4. Specify friendly name and entity ID for each

### 2. YAML Import/Export (Current)

Users can import/export configuration via YAML file:
- **Export:** Save current configuration to YAML file
- **Import:** Load configuration from YAML file

## Entity Mapping for Media Players

### Requirements for Select Source Feature

For the select source feature to work, media player entities must be mapped:

```yaml
entityMappings:
  - friendlyName: "kitchen radio"
    entityId: "media_player.kitchen_radio_2"
  - friendlyName: "bedroom speaker"
    entityId: "media_player.bedroom_sonos"
  - friendlyName: "living room tv"
    entityId: "media_player.living_room_tv"
```

### Friendly Name Guidelines

**Good friendly names:**
- "kitchen radio" - Simple, descriptive
- "bedroom speaker" - Location + device type
- "living room tv" - Location + device type
- "office stereo" - Location + device type

**Avoid:**
- "media_player.kitchen_radio_2" - Don't use entity ID as friendly name
- "the kitchen radio" - Articles are stripped during matching
- "radio in kitchen" - Prefer "kitchen radio" (location first)

### Entity ID Format

Home Assistant media player entity IDs follow the format:
```
media_player.<device_name>
```

Examples:
- `media_player.kitchen_radio_2`
- `media_player.bedroom_sonos`
- `media_player.living_room_tv`
- `media_player.spotify`

## Finding Entity IDs in Home Assistant

### Method 1: Developer Tools → States

1. Open Home Assistant web interface
2. Navigate to Developer Tools → States
3. Search for "media_player"
4. Copy the entity_id from the list

### Method 2: Entity Settings

1. Open Home Assistant web interface
2. Navigate to Settings → Devices & Services → Entities
3. Filter by "media_player"
4. Click on entity to see details
5. Copy the entity ID

### Method 3: Template Tool

Use the template tool to list all media players:
```jinja2
{% for state in states.media_player %}
  {{ state.entity_id }}: {{ state.name }}
{% endfor %}
```

## Matching Algorithm

### How Friendly Names are Matched

The skill uses fuzzy matching to find the best entity mapping:

1. **Normalize user input:**
   - Convert to lowercase
   - Remove articles (the, a, an)
   - Trim whitespace

2. **Exact match:**
   - Check if normalized input exactly matches a friendly name

3. **Contains match:**
   - Check if friendly name contains input, or input contains friendly name

Example:
- User says: "kitchen radio"
- Normalized: "kitchen radio"
- Matches: friendlyName="kitchen radio" → entityId="media_player.kitchen_radio_2"

### Multiple Matches

If multiple entities match (e.g., "radio" matches both "kitchen radio" and "bedroom radio"), the **first match** is returned.

**Best practice:** Use unique, specific friendly names to avoid ambiguity.

## Configuration Requirements for Select Source

### Minimum Configuration

To use the select source feature, users must:

1. **Configure Home Assistant connection:**
   - Base URL (e.g., "http://homeassistant.local:8123")
   - Long-lived access token

2. **Add entity mappings for media players:**
   - At least one media player entity mapping
   - Friendly name that's easy to say
   - Correct entity ID from Home Assistant

### Example Configuration

```yaml
homeAssistant:
  baseUrl: "http://192.168.1.100:8123"
  accessToken: "eyJ0eXAiOiJKV1QiLCJhbGc..."
  entityMappings:
    - friendlyName: "kitchen radio"
      entityId: "media_player.kitchen_radio_2"
```

With this configuration, users can say:
- "turn kitchen radio to BBC Radio 2"
- "set kitchen radio on Virgin Radio"
- "tune kitchen radio to Heart Dorset"

## Validation

### Entity Mapping Validation

The skill should validate:
1. **Friendly name is not empty**
2. **Entity ID is not empty**
3. **Entity ID follows format:** `<domain>.<device_name>`
4. **No duplicate friendly names** (warn user)

### Runtime Validation

When processing a command:
1. **Entity mapping exists** - Return `EntityNotMapped` if not found
2. **Entity exists in Home Assistant** - Return `EntityNotFound` if 404
3. **Entity supports source selection** - Return `NoSourceList` if no `source_list` attribute

## User Documentation

### Setup Instructions

**To use voice control for media player sources:**

1. Find your media player entity ID in Home Assistant
2. Open Dicio → Settings → Home Assistant
3. Add a new entity mapping:
   - Friendly Name: "kitchen radio" (what you'll say)
   - Entity ID: "media_player.kitchen_radio_2" (from Home Assistant)
4. Save the mapping
5. Test with: "turn kitchen radio to BBC Radio 2"

### Troubleshooting

**"I don't know about kitchen radio"**
- Entity mapping not configured
- Check Settings → Home Assistant → Entity Mappings

**"Could not find kitchen radio"**
- Entity ID doesn't exist in Home Assistant
- Verify entity ID in Home Assistant Developer Tools

**"kitchen radio does not have any available sources"**
- Media player doesn't support source selection
- Check if entity has `source_list` attribute in Home Assistant

**"Could not find source BBC Radio 2 on kitchen radio"**
- Source name doesn't match any available sources
- Check available sources in Home Assistant
- Try saying the exact source name from the list

## Implementation Notes

### Existing Infrastructure

The entity mapping infrastructure already exists and is used by other Home Assistant skill features (turn on/off, get status, etc.).

**No changes needed** to entity mapping configuration for the select source feature.

### Code References

- **Entity mapping data model:** `EntityMapping` (protobuf)
- **YAML import/export:** `HomeAssistantYamlUtils.kt`
- **Settings UI:** `HomeAssistantInfo.kt` (EntityMappingsEditor)
- **Matching algorithm:** `HomeAssistantSkill.findBestMatch()`

### Reuse for Select Source

The select source feature will use the **same entity mapping infrastructure**:

```kotlin
val mapping = findBestMatch(entityName, settings.entityMappingsList)
    ?: return HomeAssistantOutput.EntityNotMapped(entityName)

// mapping.entityId is now "media_player.kitchen_radio_2"
// Use this to query Home Assistant API
```

No additional configuration or setup required beyond existing entity mappings.

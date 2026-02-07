# Home Assistant Media Source Control - Requirements

## Overview

Enable voice control of Home Assistant media players to select from available source presets (radio stations, inputs, etc.) through natural language commands. Uses a two-stage matching process: first match the command pattern and entity, then fuzzy match the requested source name against available presets.

## User Stories

### Story 1: Play Media on Specific Device
**As a** user  
**I want to** say "turn kitchen radio to BBC Radio 2"  
**So that** the media player switches to the matching preset source without manual interaction

### Story 2: Alternative Phrasing Support
**As a** user  
**I want to** use natural variations like "play BBC Radio 2 on kitchen radio" or "tune kitchen radio to BBC Radio 2"  
**So that** I can speak naturally without memorizing exact commands

### Story 3: Fuzzy Source Matching
**As a** user  
**I want to** say "BBC Radio Two" when the preset is named "BBC Radio 2"  
**So that** minor pronunciation or transcription differences don't prevent the command from working

### Story 4: Number Homophone Handling
**As a** user  
**I want to** say "BBC Radio too" or "BBC Radio to" and have it match "BBC Radio 2"  
**So that** speech recognition errors with number homophones don't prevent the command from working

### Story 5: Error Feedback
**As a** user  
**I want to** receive clear feedback when a media player or source is not found  
**So that** I understand what went wrong and can correct my command

## Functional Requirements (EARS Notation)

### FR-1: Two-Stage Matching Process
**WHEN** a user command matches the media source pattern  
**THE SYSTEM SHALL** execute a two-stage process:
1. Match command pattern and extract entity name and requested source
2. Fetch available sources from the media player
3. Fuzzy match the requested source against available sources
4. Call the service with the best matching source

### FR-2: Sentence Pattern Recognition
**WHEN** a user provides a command matching any of these patterns:
- "(turn|switch|set|tune|change) [entity] to [source]"
- "(tune|set) [entity] on [source]"

**THE SYSTEM SHALL** extract the entity name and requested source name correctly

**Note:** Patterns using "play" and "start" verbs are excluded to avoid conflicts with the existing media skill.

### FR-3: Entity Name Extraction
**WHEN** the user specifies an entity name (e.g., "kitchen radio")  
**THE SYSTEM SHALL** convert it to the appropriate Home Assistant entity_id format (e.g., "media_player.kitchen_radio")

### FR-4: Source List Retrieval
**WHEN** the entity name is successfully matched  
**THE SYSTEM SHALL** query Home Assistant for the `source_list` attribute of the media player entity

### FR-5: Number Variation Generation
**WHEN** fuzzy matching is performed  
**THE SYSTEM SHALL** generate multiple variations of the requested source by replacing number words and homophones with their digit equivalents

**Examples:**
- "BBC Radio two" generates: "BBC Radio two", "BBC Radio 2", "BBC Radio to", "BBC Radio too"
- "Radio for" generates: "Radio for", "Radio 4", "Radio fore"

### FR-6: Fuzzy Source Matching with Variations
**WHEN** the source list is retrieved  
**THE SYSTEM SHALL** perform fuzzy matching between each generated variation and available sources, selecting the variation with the highest similarity score

### FR-7: Source Selection Service Call
**WHEN** a matching source is found  
**THE SYSTEM SHALL** call the Home Assistant `media_player.select_source` service with:
- `entity_id`: The media player entity
- `source`: The matched source name from the source_list

### FR-8: Success Feedback
**WHEN** the source is successfully selected  
**THE SYSTEM SHALL** provide speech feedback confirming the action (e.g., "Playing BBC Radio 2 on kitchen radio")

### FR-9: Error Handling - Entity Not Found
**WHEN** the specified entity does not exist in Home Assistant  
**THE SYSTEM SHALL** provide speech feedback indicating the entity was not found

### FR-10: Error Handling - No Source List
**WHEN** the media player entity has no source_list attribute or it is empty  
**THE SYSTEM SHALL** provide speech feedback indicating no sources are available for that player

### FR-11: Error Handling - No Matching Source
**WHEN** none of the generated variations match any available source above the threshold  
**THE SYSTEM SHALL** provide speech feedback indicating the source was not found

### FR-12: Error Handling - Service Call Failure
**WHEN** the Home Assistant service call fails  
**THE SYSTEM SHALL** provide speech feedback indicating the operation failed

## Non-Functional Requirements

### NFR-1: Response Time
**WHEN** a valid command is recognized  
**THE SYSTEM SHALL** initiate the Home Assistant API call within 500ms

### NFR-2: Language Support
**WHEN** the feature is implemented  
**THE SYSTEM SHALL** support English language sentences initially, with structure allowing future translations

### NFR-3: Backward Compatibility
**WHEN** the new feature is added  
**THE SYSTEM SHALL** maintain all existing Home Assistant skill functionality without breaking changes

## Acceptance Criteria

1. User can say "turn kitchen radio to BBC Radio 2" and the media player switches to that source
2. User can say "switch kitchen radio to BBC Radio 2" and the media player switches to that source
3. User can say "tune kitchen radio to BBC Radio 2" and the media player switches to that source
4. User can say "set kitchen radio to BBC Radio 2" and the media player switches to that source
5. User can say "change kitchen radio to BBC Radio 2" and the media player switches to that source
6. User can say "tune kitchen radio on BBC Radio 2" and the media player switches to that source
7. User can say "set kitchen radio on BBC Radio 2" and the media player switches to that source
8. System fuzzy matches "BBC Radio Two" to preset "BBC Radio 2" successfully
9. System provides clear error message if entity doesn't exist
10. System provides clear error message if entity has no source_list
11. System provides clear error message if no source matches the request
12. System provides clear error message if service call fails
13. System provides success confirmation when source is selected
14. All existing Home Assistant skill tests continue to pass
15. New sentence patterns are added to English sentences file
16. New patterns do not conflict with existing media skill patterns

## Example Sentences

The following sentences should all work correctly:

**Pattern 1: [action] [entity] to [source]**
- "turn kitchen radio to BBC Radio 2"
- "switch kitchen radio to BBC Radio 2"
- "set kitchen radio to BBC Radio 2"
- "tune kitchen radio to BBC Radio 2"
- "change kitchen radio to BBC Radio 2"

**Pattern 2: [action] [entity] on [source]**
- "tune kitchen radio on BBC Radio 2"
- "set kitchen radio on BBC Radio 2"

**With optional "the":**
- "turn the kitchen radio to BBC Radio 2"
- "set the kitchen radio on BBC Radio 2"

**With fuzzy matching:**
- "turn kitchen radio to BBC Radio Two" → matches "BBC Radio 2"
- "tune kitchen radio to bee bee see radio 2" → matches "BBC Radio 2"
- "set kitchen radio on radio two" → matches "BBC Radio 2"

## Out of Scope

- Automatic discovery of all media players in Home Assistant
- Volume control (handled by existing set_state commands)
- Media player state queries (handled by existing get_status)
- Playlist management
- Multi-room audio synchronization
- Direct media_content_id playback (using select_source instead)
- Custom fuzzy matching threshold configuration (use default)

## Dependencies

- Home Assistant API must be configured and accessible
- Target media player entity must exist in Home Assistant
- Media player must have a `source_list` attribute with available sources
- Existing Home Assistant skill infrastructure
- Fuzzy matching algorithm (likely already available in Dicio framework)

## Technical Notes

### Two-Stage Process Flow
1. **Stage 1: Pattern Matching**
   - User input matches sentence pattern
   - Extract entity_name and requested source_name
   
2. **Stage 2: Source Resolution**
   - Query HA API for entity state to get `source_list` attribute
   - Fuzzy match requested source_name against source_list items
   - Select best match above threshold
   - Call `media_player.select_source` service

### Sentence Definitions (YAML Format)

The following will be added to `skill_definitions.yml`:

```yaml
- id: select_source
  captures:
    - id: entity_name
      type: string
    - id: source_name
      type: string
```

The following will be added to `home_assistant.yml`:

```yaml
select_source:
  - (turn|switch|set|tune|change) (the )?.entity_name. to .source_name.
  - (tune|set) (the )?.entity_name. on .source_name.
```

**Note:** Patterns using "play" and "start" verbs are intentionally excluded to avoid conflicts with the existing media skill which uses those verbs for generic media control.

### Home Assistant Service
- Service: `media_player.select_source`
- Required parameters:
  - `entity_id`: The media player entity
  - `source`: The exact source name from the entity's source_list

### Source List Attribute
- Retrieved from entity state: `state.attributes.source_list`
- Returns array of strings (e.g., `["BBC Radio 2", "BBC Radio 4", "Spotify"]`)
- May be null or empty if player doesn't support source selection

### Fuzzy Matching
- Use existing Dicio fuzzy matching infrastructure
- Match user's requested source against each item in source_list
- Select highest scoring match above minimum threshold
- Entity name matching should follow existing Home Assistant skill patterns
- Sentence definitions follow dicio-sentences-compiler syntax

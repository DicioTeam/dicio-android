# Home Assistant Skill Implementation Tasks

## Phase 1: Sentence Definitions & Proto Setup

### Task 1.1: Add skill definition
- [ ] Edit `app/src/main/sentences/skill_definitions.yml`
- [ ] Add `home_assistant` skill with `get_status` and `set_state` sentences
- [ ] Define captures: `entity_name` (string) and `action` (string)

### Task 1.2: Create English sentences
- [ ] Create `app/src/main/sentences/en/home_assistant.yml`
- [ ] Define `get_status` sentence patterns
- [ ] Define `set_state` sentence patterns
- [ ] Test build to verify syntax

### Task 1.3: Create proto definition
- [ ] Create `app/src/main/proto/skill_settings_home_assistant.proto`
- [ ] Define `SkillSettingsHomeAssistant` message (base_url, access_token, entity_mappings)
- [ ] Define `EntityMapping` message (friendly_name, entity_id)
- [ ] Test build to verify proto compilation

## Phase 2: Core Skill Implementation

### Task 2.1: Create package structure
- [ ] Create package `org.stypox.dicio.skills.homeassistant`

### Task 2.2: Implement HomeAssistantApi.kt
- [ ] Create `HomeAssistantApi.kt`
- [ ] Implement `getEntityState()` for GET /api/states/{entity_id}
- [ ] Implement `callService()` for POST /api/services/{domain}/{service}
- [ ] Add error handling for network/auth failures

### Task 2.3: Implement HomeAssistantOutput.kt
- [ ] Create sealed interface `HomeAssistantOutput`
- [ ] Implement `GetStatusSuccess` data class with getSpeechOutput() and GraphicalOutput()
- [ ] Implement `SetStateSuccess` data class with getSpeechOutput() and GraphicalOutput()
- [ ] Implement error outputs: `EntityNotMapped`, `EntityNotFound`, `InvalidAction`, `ConnectionFailed`, `AuthFailed`
- [ ] Create Composable UI for success states

### Task 2.4: Implement HomeAssistantSkill.kt
- [ ] Create `HomeAssistantSkill` extending `StandardRecognizerSkill<HomeAssistant>`
- [ ] Implement `generateOutput()` method
- [ ] Implement fuzzy matching helper: `findBestMatch()` (normalize, exact match, contains match)
- [ ] Handle `get_status` sentence type (call getEntityState)
- [ ] Handle `set_state` sentence type:
  - [ ] Implement `parseAction()` helper (map on/off/open/close/lock/unlock/toggle to services)
  - [ ] Extract domain from entity_id
  - [ ] Map domain to service (light.turn_on, cover.open_cover, lock.lock, etc.)
  - [ ] Call callService with correct domain/service
- [ ] Add comprehensive error handling (entity not mapped, not found, connection failed, auth failed, invalid action)

### Task 2.5: Implement SkillSettingsHomeAssistantSerializer.kt
- [ ] Create proto serializer for DataStore
- [ ] Define defaultValue

## Phase 3: Settings UI

### Task 3.1: Implement HomeAssistantInfo.kt
- [ ] Create `HomeAssistantInfo` object extending `SkillInfo`
- [ ] Implement name(), sentenceExample(), icon()
- [ ] Implement isAvailable() (check Sentences.HomeAssistant[ctx.sentencesLanguage] != null)
- [ ] Implement build() to create HomeAssistantSkill instance with Sentences.HomeAssistant[ctx.sentencesLanguage]
- [ ] Create DataStore extension property (homeAssistantDataStore)
- [ ] Add SharedPreferencesMigration if needed

### Task 3.2: Implement basic settings UI
- [ ] Add `StringSetting` for base URL in renderSettings
- [ ] Add `StringSetting` for access token in renderSettings
- [ ] Add DataStore flow collection and update logic

### Task 3.3: Implement entity mappings editor
- [ ] Create `EntityMappingsEditor` Composable
- [ ] Display list of current mappings
- [ ] Implement "Add Mapping" button with dialog
- [ ] Implement edit functionality (tap to edit)
- [ ] Implement delete functionality
- [ ] Wire up to DataStore updates

## Phase 4: Integration & Resources

### Task 4.1: Register skill
- [ ] Edit `app/src/main/kotlin/org/stypox/dicio/eval/SkillHandler.kt`
- [ ] Add `HomeAssistantInfo` to `allSkillInfoList`

### Task 4.2: Add string resources
- [ ] Edit `app/src/main/res/values/strings.xml`
- [ ] Add skill metadata: skill_name_home_assistant, skill_sentence_example_home_assistant
- [ ] Add output messages: skill_homeassistant_entity_state, skill_homeassistant_set_success, skill_homeassistant_invalid_action
- [ ] Add error messages: skill_homeassistant_entity_not_mapped, skill_homeassistant_entity_not_found, skill_homeassistant_connection_failed, skill_homeassistant_auth_failed
- [ ] Add settings labels: pref_homeassistant_base_url, pref_homeassistant_access_token, pref_homeassistant_entity_mappings, pref_homeassistant_add_mapping, pref_homeassistant_friendly_name, pref_homeassistant_entity_id
- [ ] Ensure proper formatting with placeholders (%1$s, %2$s)

### Task 4.3: Add icon (optional)
- [ ] Choose appropriate Material icon or create custom icon
- [ ] Update icon() method in HomeAssistantInfo

## Phase 5: Testing & Refinement

### Task 5.1: Manual testing
- [ ] Test sentence recognition for get_status
- [ ] Test sentence recognition for set_state
- [ ] Test fuzzy matching with various entity names
- [ ] Test API calls with real Home Assistant instance
- [ ] Test all error scenarios
- [ ] Test settings UI (add/edit/delete mappings)

### Task 5.2: Edge cases
- [ ] Test with empty entity mappings
- [ ] Test with missing URL/token
- [ ] Test with invalid URL/token
- [ ] Test with unreachable Home Assistant
- [ ] Test with non-existent entities
- [ ] Test with unsupported actions for entity type
- [ ] Test with different entity domains (light, switch, cover, lock, climate, sensor, etc.)
- [ ] Test fuzzy matching edge cases (articles, partial matches, no matches)
- [ ] Test URL validation (http/https, port numbers)

### Task 5.3: UI/UX polish
- [ ] Ensure speech output is natural
- [ ] Ensure graphical output is clear and informative
- [ ] Add loading states if needed
- [ ] Improve error messages for clarity

## Phase 6: Documentation

### Task 6.1: Update README
- [ ] Add Home Assistant skill to Skills section
- [ ] Add example command
- [ ] Commit with "[HomeAssistant]" prefix

### Task 6.2: Update F-Droid description
- [ ] Edit `fastlane/metadata/android/en-US/full_description.txt`
- [ ] Add Home Assistant skill description

### Task 6.3: Add setup instructions
- [ ] Document how to get Home Assistant Long-Lived Access Token (Profile page)
- [ ] Document how to find entity IDs (Developer Tools > States)
- [ ] Add example entity mappings
- [ ] Add troubleshooting tips (connection issues, auth failures, entity not found)
- [ ] Note security considerations (HTTPS recommended, token storage)

## Implementation Order Recommendation

1. **Start with Phase 1** - Get sentences and proto working first
2. **Phase 2.2** - Implement API client (can test independently)
3. **Phase 2.4** - Implement skill logic (core functionality)
4. **Phase 2.3** - Implement outputs (display results)
5. **Phase 3.1-3.2** - Basic settings (URL + token)
6. **Phase 3.3** - Entity mappings UI (most complex)
7. **Phase 4** - Integration
8. **Phase 5** - Testing
9. **Phase 6** - Documentation

## Notes

- Test build after each proto/sentence change
- Use weather skill as reference for patterns (especially settings UI and DataStore)
- Consider creating a test Home Assistant instance for development
- Entity mappings UI is the most complex part - may need iteration
- Start with basic functionality, add polish later
- Domain-specific service mapping needs careful attention (cover uses open_cover/close_cover, lock uses lock/unlock)
- Fuzzy matching should handle articles (the, a, an) and partial matches
- Consider adding validation for URL format and entity_id format in settings UI

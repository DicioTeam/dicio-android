# Home Assistant Skill Implementation Tasks

## Phase 1: Sentence Definitions & Proto Setup ✅ COMPLETE

### Task 1.1: Add skill definition ✅
- [x] Edit `app/src/main/sentences/skill_definitions.yml`
- [x] Add `home_assistant` skill with `get_status` and `set_state` sentences
- [x] Define captures: `entity_name` (string) and `action` (string)

### Task 1.2: Create English sentences ✅
- [x] Create `app/src/main/sentences/en/home_assistant.yml`
- [x] Define `get_status` sentence patterns
- [x] Define `set_state` sentence patterns
- [x] Test build to verify syntax

### Task 1.3: Create proto definition ✅
- [x] Create `app/src/main/proto/skill_settings_home_assistant.proto`
- [x] Define `SkillSettingsHomeAssistant` message (base_url, access_token, entity_mappings)
- [x] Define `EntityMapping` message (friendly_name, entity_id)
- [x] Test build to verify proto compilation

## Phase 2: Core Skill Implementation ✅ COMPLETE

### Task 2.1: Create package structure ✅
- [x] Create package `org.stypox.dicio.skills.homeassistant`

### Task 2.2: Implement HomeAssistantApi.kt ✅
- [x] Create `HomeAssistantApi.kt`
- [x] Implement `getEntityState()` for GET /api/states/{entity_id}
- [x] Implement `callService()` for POST /api/services/{domain}/{service}
- [x] Add error handling for network/auth failures

### Task 2.3: Implement HomeAssistantOutput.kt ✅
- [x] Create sealed interface `HomeAssistantOutput`
- [x] Implement `GetStatusSuccess` data class with getSpeechOutput() and GraphicalOutput()
- [x] Implement `SetStateSuccess` data class with getSpeechOutput() and GraphicalOutput()
- [x] Implement error outputs: `EntityNotMapped`, `EntityNotFound`, `InvalidAction`, `ConnectionFailed`, `AuthFailed`
- [x] Create Composable UI for success states

### Task 2.4: Implement HomeAssistantSkill.kt ✅
- [x] Create `HomeAssistantSkill` extending `StandardRecognizerSkill<HomeAssistant>`
- [x] Implement `generateOutput()` method
- [x] Implement fuzzy matching helper: `findBestMatch()` (normalize, exact match, contains match)
- [x] Handle `get_status` sentence type (call getEntityState)
- [x] Handle `set_state` sentence type:
  - [x] Implement `parseAction()` helper (map on/off/open/close/lock/unlock/toggle to services)
  - [x] Extract domain from entity_id
  - [x] Map domain to service (light.turn_on, cover.open_cover, lock.lock, etc.)
  - [x] Call callService with correct domain/service
- [x] Add comprehensive error handling (entity not mapped, not found, connection failed, auth failed, invalid action)

### Task 2.5: Implement SkillSettingsHomeAssistantSerializer.kt ✅
- [x] Create proto serializer for DataStore
- [x] Define defaultValue

## Phase 3: Settings UI ✅ COMPLETE

### Task 3.1: Implement HomeAssistantInfo.kt ✅
- [x] Create `HomeAssistantInfo` object extending `SkillInfo`
- [x] Implement name(), sentenceExample(), icon()
- [x] Implement isAvailable() (check Sentences.HomeAssistant[ctx.sentencesLanguage] != null)
- [x] Implement build() to create HomeAssistantSkill instance with Sentences.HomeAssistant[ctx.sentencesLanguage]
- [x] Create DataStore extension property (homeAssistantDataStore)
- [x] Add SharedPreferencesMigration if needed (not needed for new skill)

### Task 3.2: Implement basic settings UI ✅
- [x] Add `StringSetting` for base URL in renderSettings
- [x] Add `StringSetting` for access token in renderSettings
- [x] Add DataStore flow collection and update logic

### Task 3.3: Implement entity mappings editor ✅
- [x] Create `EntityMappingsEditor` Composable
- [x] Display list of current mappings
- [x] Implement "Add Mapping" button with dialog
- [x] Implement edit functionality (tap to edit)
- [x] Implement delete functionality
- [x] Wire up to DataStore updates

## Phase 4: Integration & Resources ✅ COMPLETE

### Task 4.1: Register skill ✅
- [x] Edit `app/src/main/kotlin/org/stypox/dicio/eval/SkillHandler.kt`
- [x] Add `HomeAssistantInfo` to `allSkillInfoList`

### Task 4.2: Add string resources ✅
- [x] Edit `app/src/main/res/values/strings.xml`
- [x] Add skill metadata: skill_name_home_assistant, skill_sentence_example_home_assistant
- [x] Add output messages: skill_homeassistant_entity_state, skill_homeassistant_set_success, skill_homeassistant_invalid_action
- [x] Add error messages: skill_homeassistant_entity_not_mapped, skill_homeassistant_entity_not_found, skill_homeassistant_connection_failed, skill_homeassistant_auth_failed
- [x] Add settings labels: pref_homeassistant_base_url, pref_homeassistant_access_token, pref_homeassistant_entity_mappings, pref_homeassistant_add_mapping, pref_homeassistant_friendly_name, pref_homeassistant_entity_id
- [x] Ensure proper formatting with placeholders (%1$s, %2$s)

### Task 4.3: Add icon ✅
- [x] Choose appropriate Material icon (Icons.Default.Home)
- [x] Update icon() method in HomeAssistantInfo

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

## Phase 6: Documentation ✅ COMPLETE

### Task 6.1: Update README ✅
- [x] Add Home Assistant skill to Skills section
- [x] Add example command
- [ ] Commit with "[HomeAssistant]" prefix (pending user commit)

### Task 6.2: Update F-Droid description ✅
- [x] Edit `fastlane/metadata/android/en-US/full_description.txt`
- [x] Add Home Assistant skill description

### Task 6.3: Add setup instructions ⚠️ RECOMMENDED
- [ ] Document how to get Home Assistant Long-Lived Access Token (Profile page)
- [ ] Document how to find entity IDs (Developer Tools > States)
- [ ] Add example entity mappings
- [ ] Add troubleshooting tips (connection issues, auth failures, entity not found)
- [ ] Note security considerations (HTTPS recommended, token storage)

Note: Setup instructions can be added to the existing .amazonq/rules/homeassistant.md file or as a separate SETUP.md file

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

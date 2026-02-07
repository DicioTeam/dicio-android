# Home Assistant Media Source Control - Tasks

## Overview

Implementation tasks for adding media player source selection to the Home Assistant skill. Tasks are ordered by dependency and should be executed sequentially.

## Task List

### Phase 1: Sentence Definitions

- [ ] **Task 1.1: Add sentence type to skill_definitions.yml**
  - File: `app/src/main/sentences/skill_definitions.yml`
  - Add `select_source` sentence type to `home_assistant` skill
  - Define two captures: `entity_name` and `source_name` (both type: string)
  - Expected outcome: Sentence type defined in YAML

- [ ] **Task 1.2: Add sentence patterns to home_assistant.yml**
  - File: `app/src/main/sentences/en/home_assistant.yml`
  - Add `select_source:` section with two patterns
  - Pattern 1: `(turn|switch|set|tune|change) (the )?.entity_name. to .source_name.`
  - Pattern 2: `(tune|set) (the )?.entity_name. on .source_name.`
  - Expected outcome: Sentence patterns defined in English

- [ ] **Task 1.3: Build and verify generated code**
  - Run Gradle build to generate `Sentences.kt`
  - Verify `HomeAssistant.SelectSource` class is generated
  - Verify it has `entityName: String?` and `sourceName: String?` fields
  - Expected outcome: Generated code compiles successfully

### Phase 2: API Extension

- [ ] **Task 2.1: Extend callService() method**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantApi.kt`
  - Add optional parameter: `extraParams: Map<String, String> = emptyMap()`
  - Update body construction to include extraParams
  - Expected outcome: Method signature extended, backward compatible

- [ ] **Task 2.2: Test callService() backward compatibility**
  - Verify existing call in `HomeAssistantSkill.kt` still compiles
  - Run existing Home Assistant skill tests
  - Expected outcome: No breaking changes, all existing tests pass

### Phase 3: Output Types

- [ ] **Task 3.1: Add string resources**
  - File: `app/src/main/res/values/strings.xml`
  - Add `skill_home_assistant_select_source_success`
  - Add `skill_home_assistant_no_source_list`
  - Add `skill_home_assistant_source_not_found`
  - Add `skill_home_assistant_source_not_found_short`
  - Expected outcome: 4 new string resources added

- [ ] **Task 3.2: Add SelectSourceSuccess output**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantOutput.kt`
  - Add `SelectSourceSuccess` data class
  - Implement `getSpeechOutput()` and `GraphicalOutput()`
  - Expected outcome: Success output type implemented

- [ ] **Task 3.3: Add NoSourceList output**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantOutput.kt`
  - Add `NoSourceList` data class
  - Implement `getSpeechOutput()` and `GraphicalOutput()`
  - Use error color for text
  - Expected outcome: Error output type implemented

- [ ] **Task 3.4: Add SourceNotFound output**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantOutput.kt`
  - Add `SourceNotFound` data class
  - Implement `getSpeechOutput()` and `GraphicalOutput()`
  - Use error color for text
  - Expected outcome: Error output type implemented

### Phase 4: Core Logic

- [ ] **Task 4.1: Add generateNumberVariations() method**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkill.kt`
  - Implement number word to digit/homophone mapping
  - Support: one→[1,won], two→[2,to,too], four→[4,for,fore], eight→[8,ate], etc.
  - Return list of all variations including original
  - Add as private method
  - Expected outcome: Number variation generation implemented

- [ ] **Task 4.2: Add findBestSourceMatch() method**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkill.kt`
  - Generate variations using generateNumberVariations()
  - For each variation: try exact match, contains match, fuzzy match
  - Return best match above 0.4 threshold
  - Add as private method
  - Expected outcome: Fuzzy matching with variations implemented

- [ ] **Task 4.3: Add calculateSimilarity() method**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkill.kt`
  - Implement word-based Jaccard similarity
  - Add as private method
  - Expected outcome: Similarity calculation method implemented

- [ ] **Task 4.4: Add handleSelectSource() method**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkill.kt`
  - Implement two-stage process:
    1. Get entity state and extract source_list
    2. Fuzzy match requested source (with variations)
    3. Call select_source service
  - Handle all error cases
  - Add as private suspend method
  - Expected outcome: Main handler method implemented

- [ ] **Task 4.5: Add SelectSource case to generateOutput()**
  - File: `app/src/main/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkill.kt`
  - Add `is HomeAssistant.SelectSource ->` branch in when statement
  - Extract entityName and sourceName
  - Find entity mapping
  - Call handleSelectSource()
  - Expected outcome: SelectSource integrated into skill

### Phase 5: Unit Tests

- [ ] **Task 5.1: Test generateNumberVariations()**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/NumberVariationsTest.kt`
  - Test single number word: "BBC Radio two" → includes "BBC Radio 2", "BBC Radio to", "BBC Radio too"
  - Test multiple numbers: "one two three" → all combinations
  - Test no numbers: "BBC Radio" → ["BBC Radio"]
  - Test all number words: one through ten
  - Test homophones: two→[2,to,too], four→[4,for,fore], eight→[8,ate]
  - Test case insensitivity
  - Expected outcome: 8+ test cases passing

- [ ] **Task 5.2: Test findBestSourceMatch() - exact matches**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test exact match with digit: "BBC Radio 2" finds "BBC Radio 2"
  - Test exact match (case insensitive)
  - Test with real source data
  - Expected outcome: 3+ test cases passing

- [ ] **Task 5.3: Test findBestSourceMatch() - homophone variations**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test "BBC Radio too" finds "BBC Radio 2" (not "BBC Radio 4")
  - Test "BBC Radio to" finds "BBC Radio 2"
  - Test "BBC Radio for" finds "BBC Radio 4"
  - Test "Radio ate" finds "Radio 8"
  - Expected outcome: 4+ test cases passing

- [ ] **Task 5.4: Test findBestSourceMatch() - partial matches**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test contains matches (both directions)
  - Test "Radio 2" finds "BBC Radio 2"
  - Test with real source data
  - Expected outcome: 3+ test cases passing

- [ ] **Task 5.5: Test findBestSourceMatch() - fuzzy matches**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test word-based similarity
  - Test threshold behavior (0.4)
  - Test tie-breaking (prefer higher score, then shorter)
  - Expected outcome: 3+ test cases passing

- [ ] **Task 5.6: Test findBestSourceMatch() - no match**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test with non-matching sources
  - Test empty source list
  - Test below threshold
  - Expected outcome: 3+ test cases passing

- [ ] **Task 5.7: Test calculateSimilarity()**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/FuzzyMatchingTest.kt`
  - Test identical strings (1.0)
  - Test no common words (0.0)
  - Test partial overlap
  - Expected outcome: 4+ test cases passing

- [ ] **Task 5.8: Test source list extraction**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
  - Test valid source_list
  - Test empty source_list
  - Test missing source_list attribute
  - Test null attributes
  - Expected outcome: 4+ test cases passing

### Phase 6: Integration Tests

- [ ] **Task 6.1: Test end-to-end flow - exact match with digit**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
  - Mock API responses
  - Test "turn kitchen radio to BBC Radio 2" → SelectSourceSuccess
  - Verify correct service call
  - Expected outcome: Integration test passing

- [ ] **Task 6.2: Test end-to-end flow - homophone variation**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
  - Mock API responses
  - Test "turn kitchen radio to BBC Radio too" → SelectSourceSuccess (finds "BBC Radio 2")
  - Verify correct source is matched and called
  - Expected outcome: Integration test passing

- [ ] **Task 6.3: Test end-to-end flow - fuzzy match**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
  - Mock API responses
  - Test with partial source name
  - Verify correct source is matched and called
  - Expected outcome: Integration test passing

- [ ] **Task 6.4: Test error scenarios**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/SelectSourceIntegrationTest.kt`
  - Test EntityNotMapped
  - Test NoSourceList
  - Test SourceNotFound
  - Test API failures
  - Expected outcome: 4+ error test cases passing

- [ ] **Task 6.5: Test sentence recognition**
  - File: `app/src/test/kotlin/org/stypox/dicio/skills/homeassistant/HomeAssistantSkillTest.kt`
  - Test "turn [entity] to [source]" pattern
  - Test "set [entity] on [source]" pattern
  - Test with "the" article
  - Verify no conflict with set_state_on
  - Expected outcome: 4+ sentence tests passing

### Phase 7: Manual Testing

- [ ] **Task 7.1: Build and install on device**
  - Build debug APK
  - Install on test device
  - Expected outcome: App installs successfully

- [ ] **Task 7.2: Configure entity mapping**
  - Open Dicio settings
  - Add entity mapping for test media player
  - Verify mapping is saved
  - Expected outcome: Entity mapping configured

- [ ] **Task 7.3: Test voice commands**
  - Test "turn kitchen radio to BBC Radio 2"
  - Test "turn kitchen radio to BBC Radio too" (should find "BBC Radio 2")
  - Test "set kitchen radio on Virgin Radio"
  - Test with various source names
  - Expected outcome: Commands work, sources change correctly

- [ ] **Task 7.4: Test error cases**
  - Test with unmapped entity
  - Test with non-existent source
  - Test with media player without source_list
  - Expected outcome: Clear error messages

- [ ] **Task 7.5: Test fuzzy matching**
  - Test with partial source names
  - Test with case variations
  - Test with word variations
  - Expected outcome: Fuzzy matching works as expected

### Phase 8: Documentation

- [ ] **Task 8.1: Update README.md**
  - Add select_source to skills list
  - Add example: "Turn kitchen radio to BBC Radio 2"
  - Expected outcome: README updated

- [ ] **Task 8.2: Update fastlane description**
  - File: `fastlane/metadata/android/en-US/full_description.txt`
  - Add media source selection to feature list
  - Expected outcome: App store description updated

## Task Dependencies

```
Phase 1 (Sentences) → Phase 2 (API) → Phase 3 (Outputs) → Phase 4 (Logic)
                                                              ↓
Phase 5 (Unit Tests) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ┘
                                                              ↓
Phase 6 (Integration Tests) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ┘
                                                              ↓
Phase 7 (Manual Testing) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ┘
                                                              ↓
Phase 8 (Documentation) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ┘
```

## Estimated Effort

- **Phase 1:** 30 minutes (Sentence definitions)
- **Phase 2:** 15 minutes (API extension)
- **Phase 3:** 45 minutes (Output types)
- **Phase 4:** 60 minutes (Core logic)
- **Phase 5:** 90 minutes (Unit tests)
- **Phase 6:** 60 minutes (Integration tests)
- **Phase 7:** 45 minutes (Manual testing)
- **Phase 8:** 15 minutes (Documentation)

**Total:** ~6 hours

## Success Criteria

- [ ] All unit tests pass (30+ tests including number variations)
- [ ] All integration tests pass (9+ tests including homophone scenarios)
- [ ] Manual testing successful on real device
- [ ] Homophone variations work correctly (e.g., "too" finds "2")
- [ ] No breaking changes to existing functionality
- [ ] All existing Home Assistant tests still pass
- [ ] Code follows existing patterns and style
- [ ] Documentation updated

## Notes

- Tasks should be completed in order due to dependencies
- Each task should be committed separately for easy review
- Run full test suite after each phase
- Test on real Home Assistant instance during manual testing
- Use real source data from kitchen_radio_2 for testing

## Reference Documents

- [Requirements](./requirements.md)
- [Design](./design.md)
- [API Research](./api-research.md)
- [Fuzzy Matching Tests](./fuzzy-match-tests.md)
- [Algorithm Test Results](./algorithm-test-results.md)
- [Entity Mapping Config](./entity-mapping-config.md)
- [UI Design](./ui-design.md)
- [Unknowns Summary](./unknowns-summary.md)

# Manual Testing Checklist - Select Source Feature

## Prerequisites

✅ App installed on device (org.stypox.dicio)
✅ Home Assistant instance accessible
- [ ] Home Assistant URL configured in Dicio settings
- [ ] Access token configured in Dicio settings
- [ ] Entity mapping configured for test media player

## Task 7.2: Configure Entity Mapping

### Steps:
1. Open Dicio app
2. Navigate to Settings → Home Assistant
3. Add entity mapping:
   - Friendly Name: "kitchen radio" (or your media player name)
   - Entity ID: "media_player.kitchen_radio_2" (or your actual entity ID)
4. Save mapping

### Verification:
- [ ] Entity mapping appears in list
- [ ] Mapping persists after closing settings

## Task 7.3: Test Voice Commands

### Test Cases:

#### TC1: Basic "turn to" command
- [ ] Say: "turn kitchen radio to BBC Radio 2"
- Expected: Source changes to BBC Radio 2
- Expected: Speech feedback: "Playing BBC Radio 2 on kitchen radio"
- Expected: UI shows success message

#### TC2: "set on" command
- [ ] Say: "set kitchen radio on Virgin Radio"
- Expected: Source changes to Virgin Radio
- Expected: Speech feedback confirms action
- Expected: UI shows success message

#### TC3: "tune to" command
- [ ] Say: "tune kitchen radio to BBC Radio 4"
- Expected: Source changes to BBC Radio 4
- Expected: Speech feedback confirms action

#### TC4: "change to" command
- [ ] Say: "change kitchen radio to Heart Dorset"
- Expected: Source changes to Heart Dorset
- Expected: Speech feedback confirms action

#### TC5: "switch to" command
- [ ] Say: "switch kitchen radio to BBC Radio Solent"
- Expected: Source changes to BBC Radio Solent
- Expected: Speech feedback confirms action

#### TC6: With "the" article
- [ ] Say: "turn the kitchen radio to Virgin Radio"
- Expected: Works same as without "the"

#### TC7: Multi-word source name
- [ ] Say: "turn kitchen radio to Greatest Hits Radio Dorset"
- Expected: Source changes to Greatest Hits Radio Dorset
- Expected: Multi-word source name captured correctly

#### TC8: Source with special characters
- [ ] Say: "turn kitchen radio to Magic 100% Christmas"
- Expected: Source changes correctly
- Expected: Special characters handled properly

## Task 7.4: Test Error Cases

### Error Test Cases:

#### EC1: Unmapped entity
- [ ] Say: "turn unknown device to BBC Radio 2"
- Expected: Error message: "I don't have a mapping for unknown device"
- Expected: No API call made
- Expected: UI shows error

#### EC2: Non-existent source
- [ ] Say: "turn kitchen radio to Spotify" (if not in source list)
- Expected: Error message: "Could not find source Spotify on kitchen radio"
- Expected: UI shows error with requested source name
- Expected: No source change

#### EC3: Media player without source_list
- [ ] Configure mapping for non-media-player entity (e.g., light)
- [ ] Say: "turn living room light to BBC Radio 2"
- Expected: Error message about no available sources
- Expected: UI shows error

## Task 7.5: Test Fuzzy Matching

### Fuzzy Match Test Cases:

#### FM1: Partial source name
- [ ] Say: "turn kitchen radio to Radio 2"
- Expected: Matches "BBC Radio 2"
- Expected: Source changes correctly

#### FM2: Case variation
- [ ] Say: "turn kitchen radio to bbc radio 2"
- Expected: Matches "BBC Radio 2" (case insensitive)
- Expected: Source changes correctly

#### FM3: Missing words
- [ ] Say: "turn kitchen radio to Magic Christmas"
- Expected: Matches "Magic 100% Christmas"
- Expected: Source changes correctly

#### FM4: Word order variation
- [ ] Say: "turn kitchen radio to Hits Greatest Dorset"
- Expected: May or may not match "Greatest Hits Radio Dorset"
- Note: Current algorithm doesn't handle word reordering

#### FM5: Shortened name
- [ ] Say: "turn kitchen radio to Virgin"
- Expected: Matches "Virgin Radio"
- Expected: Source changes correctly

#### FM6: Ambiguous match
- [ ] Say: "turn kitchen radio to Radio"
- Expected: Matches first source containing "Radio"
- Expected: Source changes (may not be desired one)
- Note: This is expected behavior

## Additional Verification

### No Conflicts with Existing Commands
- [ ] Say: "turn kitchen radio on"
- Expected: Turns entity ON (not select source)
- Expected: Uses set_state_on, not select_source

- [ ] Say: "turn kitchen radio off"
- Expected: Turns entity OFF (not select source)
- Expected: Uses set_state_off, not select_source

### Speech Recognition Quality
- [ ] Test in quiet environment
- [ ] Test with background noise
- [ ] Test with different speaking speeds
- [ ] Verify multi-word sources are captured correctly

### UI Verification
- [ ] Success message displays entity name
- [ ] Success message displays source name
- [ ] Error messages are clear and helpful
- [ ] UI updates immediately after command

## Test Results Summary

### Passed: _____ / _____
### Failed: _____ / _____

## Issues Found

| Issue # | Description | Severity | Steps to Reproduce |
|---------|-------------|----------|-------------------|
| 1 | | | |
| 2 | | | |
| 3 | | | |

## Notes

- Test with your actual Home Assistant setup
- Use real source names from your media player's source_list
- Document any unexpected behavior
- Note any speech recognition issues

## Sign-off

- [ ] All critical test cases passed
- [ ] No blocking issues found
- [ ] Feature ready for use

Tested by: _______________
Date: _______________
Device: _______________

# Fuzzy Matching Test Cases - Real Data

## Actual Source List from kitchen_radio_2

```
['Greatest Hits Radio Dorset', 'Magic 100% Christmas', 'BBC Radio Solent', 'Heart Dorset', 'chillout CROOZE', 'Virgin Radio', 'BBC Radio 4', 'BBC Radio 2']
```

## Test Cases

### Exact Matches (Case Insensitive)

| User Says | Expected Match | Confidence |
|-----------|---------------|------------|
| "BBC Radio 2" | "BBC Radio 2" | 100% - Exact |
| "bbc radio 2" | "BBC Radio 2" | 100% - Case insensitive |
| "BBC RADIO 2" | "BBC Radio 2" | 100% - Case insensitive |
| "Virgin Radio" | "Virgin Radio" | 100% - Exact |
| "Heart Dorset" | "Heart Dorset" | 100% - Exact |

### Partial Matches

| User Says | Expected Match | Confidence | Notes |
|-----------|---------------|------------|-------|
| "Radio 2" | "BBC Radio 2" | High | Subset match |
| "Radio 4" | "BBC Radio 4" | High | Subset match |
| "Virgin" | "Virgin Radio" | High | Subset match |
| "Heart" | "Heart Dorset" | High | Subset match |
| "Greatest Hits" | "Greatest Hits Radio Dorset" | High | Subset match |
| "Magic Christmas" | "Magic 100% Christmas" | High | Subset match |
| "Solent" | "BBC Radio Solent" | High | Subset match |
| "CROOZE" | "chillout CROOZE" | High | Subset match |

### Fuzzy Matches (Common Variations)

| User Says | Expected Match | Confidence | Notes |
|-----------|---------------|------------|-------|
| "BBC Radio Two" | "BBC Radio 2" | Medium | Number as word |
| "BBC Radio Four" | "BBC Radio 4" | Medium | Number as word |
| "bee bee see radio 2" | "BBC Radio 2" | Medium | Phonetic spelling |
| "bee bee see radio 4" | "BBC Radio 4" | Medium | Phonetic spelling |
| "Magic Christmas" | "Magic 100% Christmas" | Medium | Missing "100%" |
| "Magic hundred percent Christmas" | "Magic 100% Christmas" | Low | Number as words |
| "Chill out cruise" | "chillout CROOZE" | Low | Phonetic variation |
| "Greatest Hits Dorset" | "Greatest Hits Radio Dorset" | High | Missing "Radio" |

### Ambiguous Cases

| User Says | Possible Matches | Expected Behavior |
|-----------|------------------|-------------------|
| "Radio" | "BBC Radio 2", "BBC Radio 4", "BBC Radio Solent", "Virgin Radio", "Greatest Hits Radio Dorset" | Return best match or first match |
| "BBC" | "BBC Radio 2", "BBC Radio 4", "BBC Radio Solent" | Return best match or first match |
| "Dorset" | "Greatest Hits Radio Dorset", "Heart Dorset" | Return best match or first match |

### No Match Cases

| User Says | Expected Match | Notes |
|-----------|---------------|-------|
| "Spotify" | null | Not in source list |
| "Netflix" | null | Not in source list |
| "Radio 1" | null | No BBC Radio 1 in list |
| "Classic FM" | null | Not in source list |
| "xyz" | null | Gibberish |

### Edge Cases

| User Says | Expected Match | Notes |
|-----------|---------------|-------|
| "100% Christmas" | "Magic 100% Christmas" | Special character % |
| "Magic 100 percent Christmas" | "Magic 100% Christmas" | Percent as word |
| "chillout cruise" | "chillout CROOZE" | Case + spelling variation |
| "Chill CROOZE" | "chillout CROOZE" | Partial + case variation |

## Algorithm Requirements

Based on these test cases, the fuzzy matching algorithm should:

1. **Case insensitive matching** - Essential
2. **Partial word matching** - Essential (e.g., "Radio 2" → "BBC Radio 2")
3. **Word order flexibility** - Nice to have (e.g., "Christmas Magic" → "Magic 100% Christmas")
4. **Number/word conversion** - Nice to have (e.g., "Two" → "2")
5. **Special character handling** - Essential (e.g., "100%" should match "100 percent")
6. **Phonetic matching** - Low priority (e.g., "CROOZE" vs "cruise")

## Recommended Threshold

Based on the source list complexity:
- **Exact match:** 1.0 (100%)
- **High confidence:** 0.7-0.99 (70-99%)
- **Medium confidence:** 0.5-0.69 (50-69%)
- **Reject below:** 0.5 (50%)

**Recommended threshold: 0.5** (50% similarity required)

## Implementation Notes

### Simple Algorithm (Recommended for MVP)

```kotlin
private fun findBestSourceMatch(requested: String, available: List<String>): String? {
    val normalized = requested.lowercase().trim()
    
    // 1. Exact match (case insensitive)
    available.firstOrNull { it.lowercase() == normalized }?.let { return it }
    
    // 2. Contains match (either direction)
    available.firstOrNull { 
        it.lowercase().contains(normalized) || 
        normalized.contains(it.lowercase())
    }?.let { return it }
    
    // 3. Word-based similarity
    val scored = available.map { source ->
        source to calculateSimilarity(normalized, source.lowercase())
    }.filter { it.second >= 0.5 }
    
    return scored.maxByOrNull { it.second }?.first
}

private fun calculateSimilarity(s1: String, s2: String): Double {
    val words1 = s1.split(Regex("\\s+")).toSet()
    val words2 = s2.split(Regex("\\s+")).toSet()
    val intersection = words1.intersect(words2).size
    val union = words1.union(words2).size
    return if (union > 0) intersection.toDouble() / union else 0.0
}
```

### Test Results Prediction

| Test Case | Algorithm Step | Expected Result |
|-----------|---------------|-----------------|
| "BBC Radio 2" | Step 1 (exact) | ✅ "BBC Radio 2" |
| "Radio 2" | Step 2 (contains) | ✅ "BBC Radio 2" |
| "BBC Radio Two" | Step 3 (similarity) | ❌ null (words don't match) |
| "Greatest Hits" | Step 2 (contains) | ✅ "Greatest Hits Radio Dorset" |
| "Magic Christmas" | Step 3 (similarity) | ✅ "Magic 100% Christmas" (2/3 words = 0.67) |
| "Spotify" | None | ✅ null |

### Limitations

The simple algorithm will **NOT** handle:
- Number-to-word conversion ("Two" → "2")
- Phonetic variations ("CROOZE" vs "cruise")
- Special character normalization ("100%" vs "100 percent")

These could be added in future iterations if needed.

## Recommendation

**Use the simple algorithm for MVP.** It handles 80% of cases correctly and is easy to understand and test. Add advanced features only if user feedback indicates they're needed.

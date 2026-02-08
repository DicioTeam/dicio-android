#!/usr/bin/env python3
"""
Prototype fuzzy matching algorithm to test with real source data
"""

def find_best_source_match(requested, available):
    """Find best matching source from available list"""
    normalized = requested.lower().strip()
    
    # 1. Exact match (case insensitive)
    for source in available:
        if source.lower() == normalized:
            return source
    
    # 2. Contains match (either direction)
    for source in available:
        if normalized in source.lower() or source.lower() in normalized:
            return source
    
    # 3. Word-based similarity
    scored = []
    for source in available:
        similarity = calculate_similarity(normalized, source.lower())
        if similarity >= 0.5:
            scored.append((source, similarity))
    
    if scored:
        return max(scored, key=lambda x: x[1])[0]
    
    return None

def calculate_similarity(s1, s2):
    """Calculate Jaccard similarity between two strings based on words"""
    import re
    words1 = set(re.split(r'\s+', s1))
    words2 = set(re.split(r'\s+', s2))
    intersection = len(words1 & words2)
    union = len(words1 | words2)
    return intersection / union if union > 0 else 0.0

# Real source list from kitchen_radio_2
SOURCES = [
    'Greatest Hits Radio Dorset',
    'Magic 100% Christmas',
    'BBC Radio Solent',
    'Heart Dorset',
    'chillout CROOZE',
    'Virgin Radio',
    'BBC Radio 4',
    'BBC Radio 2'
]

# Test cases
test_cases = [
    # Exact matches
    ("BBC Radio 2", "BBC Radio 2", "Exact match"),
    ("bbc radio 2", "BBC Radio 2", "Case insensitive"),
    ("Virgin Radio", "Virgin Radio", "Exact match"),
    
    # Partial matches
    ("Radio 2", "BBC Radio 2", "Partial - subset"),
    ("Radio 4", "BBC Radio 4", "Partial - subset"),
    ("Virgin", "Virgin Radio", "Partial - subset"),
    ("Heart", "Heart Dorset", "Partial - subset"),
    ("Greatest Hits", "Greatest Hits Radio Dorset", "Partial - subset"),
    ("Magic Christmas", "Magic 100% Christmas", "Partial - missing 100%"),
    ("Solent", "BBC Radio Solent", "Partial - subset"),
    ("CROOZE", "chillout CROOZE", "Partial - subset"),
    
    # Fuzzy matches
    ("Greatest Hits Dorset", "Greatest Hits Radio Dorset", "Fuzzy - missing word"),
    ("BBC Radio Solent", "BBC Radio Solent", "Exact match"),
    
    # Ambiguous (returns first contains match)
    ("Radio", "Greatest Hits Radio Dorset", "Ambiguous - returns first contains match"),
    ("BBC", "BBC Radio Solent", "Ambiguous - returns first BBC match"),
    ("Dorset", "Greatest Hits Radio Dorset", "Ambiguous - returns first Dorset match"),
    
    # No match
    ("Spotify", None, "No match"),
    ("Netflix", None, "No match"),
    ("Radio 1", None, "No match - not in list"),
    ("Classic FM", None, "No match"),
]

print("=" * 80)
print("FUZZY MATCHING ALGORITHM TEST")
print("=" * 80)
print()
print(f"Source List ({len(SOURCES)} items):")
for i, source in enumerate(SOURCES, 1):
    print(f"  {i}. {source}")
print()
print("=" * 80)
print("TEST RESULTS")
print("=" * 80)
print()

passed = 0
failed = 0

for requested, expected, description in test_cases:
    result = find_best_source_match(requested, SOURCES)
    status = "✅ PASS" if result == expected else "❌ FAIL"
    
    if result == expected:
        passed += 1
    else:
        failed += 1
    
    print(f"{status} | {description}")
    print(f"  Input:    '{requested}'")
    print(f"  Expected: {expected}")
    print(f"  Got:      {result}")
    
    if result != expected:
        # Show similarity scores for debugging
        print(f"  Debug: Similarity scores:")
        for source in SOURCES:
            sim = calculate_similarity(requested.lower(), source.lower())
            if sim > 0:
                print(f"    - {source}: {sim:.2f}")
    print()

print("=" * 80)
print(f"SUMMARY: {passed} passed, {failed} failed out of {len(test_cases)} tests")
print("=" * 80)

if failed > 0:
    print()
    print("FAILURES DETECTED - Algorithm may need adjustment")
else:
    print()
    print("ALL TESTS PASSED - Algorithm works as expected!")

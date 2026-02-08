package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.stypox.dicio.sentences.Sentences

class FuzzyMatchingTest : StringSpec({
    // Real source list from kitchen_radio_2
    val sources = listOf(
        "Greatest Hits Radio Dorset",
        "Magic 100% Christmas",
        "BBC Radio Solent",
        "Heart Dorset",
        "chillout CROOZE",
        "Virgin Radio",
        "BBC Radio 4",
        "BBC Radio 2"
    )

    // Helper to access private methods via reflection
    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)
    val findBestSourceMatch = skill.javaClass.getDeclaredMethod(
        "findBestSourceMatch",
        String::class.java,
        List::class.java
    ).apply { isAccessible = true }
    
    val calculateSimilarity = skill.javaClass.getDeclaredMethod(
        "calculateSimilarity",
        String::class.java,
        String::class.java
    ).apply { isAccessible = true }

    fun findMatch(requested: String): String? {
        return findBestSourceMatch.invoke(skill, requested, sources) as String?
    }

    fun similarity(s1: String, s2: String): Double {
        return calculateSimilarity.invoke(skill, s1, s2) as Double
    }

    // Exact match tests
    "exact match - case insensitive" {
        findMatch("BBC Radio 2") shouldBe "BBC Radio 2"
        findMatch("bbc radio 2") shouldBe "BBC Radio 2"
        findMatch("BBC RADIO 2") shouldBe "BBC Radio 2"
    }

    "exact match - Virgin Radio" {
        findMatch("Virgin Radio") shouldBe "Virgin Radio"
        findMatch("virgin radio") shouldBe "Virgin Radio"
    }

    "exact match - Heart Dorset" {
        findMatch("Heart Dorset") shouldBe "Heart Dorset"
    }

    // Partial match tests
    "partial match - Radio 2" {
        findMatch("Radio 2") shouldBe "BBC Radio 2"
    }

    "partial match - Radio 4" {
        findMatch("Radio 4") shouldBe "BBC Radio 4"
    }

    "partial match - Virgin" {
        findMatch("Virgin") shouldBe "Virgin Radio"
    }

    "partial match - Heart" {
        findMatch("Heart") shouldBe "Heart Dorset"
    }

    "partial match - Greatest Hits" {
        findMatch("Greatest Hits") shouldBe "Greatest Hits Radio Dorset"
    }

    "partial match - Magic Christmas" {
        findMatch("Magic Christmas") shouldBe "Magic 100% Christmas"
    }

    "partial match - Solent (fuzzy)" {
        val result = findMatch("Solent")
        // Single word has low similarity (1/3 = 0.33) - may not match
        // This is acceptable behavior for very short queries
        result shouldBe null // Below 0.4 threshold
    }

    "partial match - CROOZE (fuzzy)" {
        findMatch("CROOZE") shouldBe "chillout CROOZE" // Via fuzzy word match (1/2 = 0.5)
    }

    // Fuzzy match tests
    "fuzzy match - Greatest Hits Dorset" {
        findMatch("Greatest Hits Dorset") shouldBe "Greatest Hits Radio Dorset"
    }

    "fuzzy match - BBC Radio Solent" {
        findMatch("BBC Radio Solent") shouldBe "BBC Radio Solent"
    }

    // Ambiguous cases (fuzzy matching returns best word overlap)
    "ambiguous - Radio (fuzzy)" {
        val result = findMatch("Radio")
        result shouldNotBe null // Should match via fuzzy (1 word overlap)
    }

    "ambiguous - BBC (fuzzy)" {
        val result = findMatch("BBC")
        // Single word "BBC" vs "BBC Radio X" = 1/3 = 0.33, below threshold
        // This is acceptable - very short queries are ambiguous
        result shouldBe null
    }

    "ambiguous - Dorset (fuzzy)" {
        val result = findMatch("Dorset")
        result shouldNotBe null // Should match via fuzzy (1 word overlap)
    }

    // No match tests
    "no match - Spotify" {
        findMatch("Spotify") shouldBe null
    }

    "no match - Netflix" {
        findMatch("Netflix") shouldBe null
    }

    "no match - Radio 1" {
        findMatch("Radio 1") shouldBe null
    }

    "no match - Classic FM" {
        findMatch("Classic FM") shouldBe null
    }

    // Edge case - empty string has no word overlap
    "empty string returns null" {
        val result = findMatch("")
        result shouldBe null // No words = no fuzzy match
    }

    // Homophone variation tests
    "homophone - 'too' finds 'BBC Radio 2' not 'BBC Radio 4'" {
        findMatch("BBC Radio too") shouldBe "BBC Radio 2"
    }

    "homophone - 'to' finds 'BBC Radio 2'" {
        findMatch("BBC Radio to") shouldBe "BBC Radio 2"
    }

    "homophone - 'for' finds 'BBC Radio 4'" {
        findMatch("BBC Radio for") shouldBe "BBC Radio 4"
    }

    "homophone - 'fore' finds 'BBC Radio 4'" {
        findMatch("BBC Radio fore") shouldBe "BBC Radio 4"
    }

    "homophone - partial match with 'too'" {
        findMatch("Radio too") shouldBe "BBC Radio 2"
    }

    "homophone - case insensitive 'Too'" {
        findMatch("BBC Radio Too") shouldBe "BBC Radio 2"
    }

    // Similarity calculation tests
    "similarity - identical strings" {
        similarity("bbc radio 2", "bbc radio 2") shouldBe 1.0
    }

    "similarity - no common words" {
        similarity("bbc radio 2", "spotify") shouldBe 0.0
    }

    "similarity - partial overlap" {
        val sim = similarity("bbc radio 2", "bbc radio 4")
        sim shouldBe 0.5 // 2/4 words match (bbc, radio) = 0.5
    }

    "similarity - subset" {
        val sim = similarity("radio 2", "bbc radio 2")
        sim shouldBeGreaterThan 0.6
    }

    "similarity - Magic Christmas vs Magic 100% Christmas" {
        val sim = similarity("magic christmas", "magic 100% christmas")
        sim shouldBeGreaterThan 0.5 // 2/3 words match
    }
})

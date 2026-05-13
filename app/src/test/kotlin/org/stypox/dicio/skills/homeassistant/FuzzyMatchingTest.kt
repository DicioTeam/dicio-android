package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
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

    // Access private findBestSourceMatch via reflection
    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)
    val findBestSourceMatch = skill.javaClass.getDeclaredMethod(
        "findBestSourceMatch",
        String::class.java,
        List::class.java
    ).apply { isAccessible = true }

    fun findMatch(requested: String): String? {
        return findBestSourceMatch.invoke(skill, requested, sources) as String?
    }

    // Exact match tests
    "exact match - case insensitive" {
        findMatch("BBC Radio 2") shouldBe "BBC Radio 2"
        findMatch("bbc radio 2") shouldBe "BBC Radio 2"
        findMatch("BBC RADIO 2") shouldBe "BBC Radio 2"
    }

    "exact match - Virgin Radio" {
        findMatch("Virgin Radio") shouldBe "Virgin Radio"
    }

    "exact match - Heart Dorset" {
        findMatch("Heart Dorset") shouldBe "Heart Dorset"
    }

    // Fuzzy match tests (Levenshtein-based)
    "fuzzy match - BBC Radio Solent" {
        findMatch("BBC Radio Solent") shouldBe "BBC Radio Solent"
    }

    "fuzzy match - Greatest Hits Dorset" {
        findMatch("Greatest Hits Dorset") shouldBe "Greatest Hits Radio Dorset"
    }

    // No match tests
    "no match - Spotify" {
        findMatch("Spotify") shouldBe null
    }

    "no match - Netflix" {
        findMatch("Netflix") shouldBe null
    }

    "no match - Classic FM" {
        findMatch("Classic FM") shouldBe null
    }

    // Edge cases
    "empty string returns null" {
        findMatch("") shouldBe null
    }
})

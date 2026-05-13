package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.dicio.numbers.ParserFormatter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.context.SpeechOutputDevice
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.util.MatchHelper
import org.stypox.dicio.sentences.Sentences
import java.util.Locale

/**
 * Tests for normalizeNumberWords which uses dicio-numbers [ParserFormatter]
 * to convert spoken number words to digits (e.g. "two" -> "2").
 *
 * Note: homophone variations (e.g. "too" -> "2") are not supported by dicio-numbers
 * and would need to be handled at the dicio-numbers level in the future.
 */
class NumberVariationsTest : StringSpec({

    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)

    // Access private normalizeNumberWords via reflection
    val normalizeNumberWords = skill.javaClass.getDeclaredMethod(
        "normalizeNumberWords",
        SkillContext::class.java,
        String::class.java
    ).apply { isAccessible = true }

    // Create a SkillContext with a real ParserFormatter for English
    val ctx = object : SkillContext {
        override val parserFormatter = ParserFormatter(Locale.ENGLISH)
        override var standardMatchHelper: MatchHelper? = null
        override val android get() = throw NotImplementedError()
        override val locale get() = Locale.ENGLISH
        override val sentencesLanguage get() = "en"
        override val speechOutputDevice: SpeechOutputDevice get() = throw NotImplementedError()
        override val previousOutput: SkillOutput get() = throw NotImplementedError()
    }

    fun normalize(input: String): String {
        return normalizeNumberWords.invoke(skill, ctx, input) as String
    }

    "number word - two becomes 2" {
        normalize("BBC Radio two") shouldBe "BBC Radio 2"
    }

    "number word - four becomes 4" {
        normalize("BBC Radio four") shouldBe "BBC Radio 4"
    }

    "no number words - unchanged" {
        normalize("BBC Radio") shouldBe "BBC Radio"
    }

    "number word at start" {
        normalize("two BBC Radio") shouldBe "2 BBC Radio"
    }
})

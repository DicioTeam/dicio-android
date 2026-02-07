package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.stypox.dicio.sentences.Sentences

class NumberVariationsTest : StringSpec({
    
    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)
    
    // Use reflection to access private method
    val generateNumberVariations = skill.javaClass.getDeclaredMethod(
        "generateNumberVariations",
        String::class.java
    ).apply { isAccessible = true }
    
    fun generate(input: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        return generateNumberVariations.invoke(skill, input) as List<String>
    }
    
    "single number word - two" {
        val result = generate("BBC Radio two")
        result shouldContain "BBC Radio two"
        result shouldContain "BBC Radio 2"
        result shouldContain "BBC Radio to"
        result shouldContain "BBC Radio too"
        result shouldHaveSize 4
    }
    
    "single number word - four" {
        val result = generate("BBC Radio four")
        result shouldContain "BBC Radio four"
        result shouldContain "BBC Radio 4"
        result shouldContain "BBC Radio for"
        result shouldContain "BBC Radio fore"
        result shouldHaveSize 4
    }
    
    "single number word - eight" {
        val result = generate("Radio eight")
        result shouldContain "Radio eight"
        result shouldContain "Radio 8"
        result shouldContain "Radio ate"
        result shouldHaveSize 3
    }
    
    "no number words" {
        val result = generate("BBC Radio")
        result shouldBe listOf("BBC Radio")
    }
    
    "multiple numbers" {
        val result = generate("one two")
        result shouldContain "one two"
        result shouldContain "1 two"
        result shouldContain "won two"
        result shouldContain "one 2"
        result shouldContain "one to"
        result shouldContain "one too"
        result.size shouldBeGreaterThan 5
    }
    
    "case insensitive" {
        val result = generate("BBC Radio Two")
        result shouldContain "BBC Radio Two"
        result shouldContain "BBC Radio 2"
        result shouldContain "BBC Radio to"
        result shouldContain "BBC Radio too"
    }
    
    "number at start" {
        val result = generate("two BBC Radio")
        result shouldContain "2 BBC Radio"
        result shouldContain "to BBC Radio"
        result shouldContain "too BBC Radio"
    }
    
    "all number words have variations" {
        val numbers = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        for (number in numbers) {
            val result = generate("Radio $number")
            result.size shouldBeGreaterThan 1
        }
    }
})

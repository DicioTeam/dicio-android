package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.stypox.dicio.sentences.Sentences

/**
 * Integration tests for SelectSource feature.
 * Focuses on sentence recognition and pattern matching.
 */
class SelectSourceIntegrationTest : StringSpec({

    "sentence recognition - turn to pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio to BBC Radio 2")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio 2"
    }

    "sentence recognition - set on pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("set kitchen radio on Virgin Radio")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "Virgin Radio"
    }

    "sentence recognition - tune to pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("tune bedroom speaker to Heart Dorset")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "bedroom speaker"
        selectSource.sourceName?.trim() shouldBe "Heart Dorset"
    }

    "sentence recognition - change to pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("change living room tv to HDMI 1")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "living room tv"
        selectSource.sourceName?.trim() shouldBe "HDMI 1"
    }

    "sentence recognition - switch to pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("switch office stereo to Spotify")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "office stereo"
        selectSource.sourceName?.trim() shouldBe "Spotify"
    }

    "sentence recognition - with the article" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn the kitchen radio to BBC Radio 2")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio 2"
    }

    "sentence recognition - tune on pattern" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("tune kitchen radio on BBC Radio 4")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio 4"
    }

    "sentence recognition - set on pattern with the" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("set the bedroom speaker on Virgin Radio")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "bedroom speaker"
        selectSource.sourceName?.trim() shouldBe "Virgin Radio"
    }

    "sentence recognition - multi-word entity and source" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn living room smart speaker to Greatest Hits Radio Dorset")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "living room smart speaker"
        selectSource.sourceName?.trim() shouldBe "Greatest Hits Radio Dorset"
    }

    "sentence recognition - source with special characters" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio to Magic 100% Christmas")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "Magic 100% Christmas"
    }

    "sentence recognition - homophone 'too' instead of '2'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio to BBC Radio too")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio too"
    }

    "sentence recognition - homophone 'for' instead of '4'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio to BBC Radio for")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio for"
    }

    "sentence recognition - does not conflict with set_state_on" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio on")
        
        // Should match set_state_on, not select_source
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "kitchen radio"
    }

    "sentence recognition - does not conflict with set_state_off" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn kitchen radio off")
        
        // Should match set_state_off, not select_source
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "kitchen radio"
    }
})

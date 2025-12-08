package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.stypox.dicio.sentences.Sentences

class HomeAssistantSkillTest : StringSpec({
    "parse 'turn outside lights off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn outside lights off")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "outside lights"
    }

    "parse 'turn the kitchen light on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn the kitchen light on")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "kitchen light"
    }

    "parse 'switch bedroom lamp off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("switch bedroom lamp off")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "bedroom lamp"
    }

    "parse 'get status of living room light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("get status of living room light")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "living room light"
    }

    "parse 'what is the status for garage door'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("what is the status for garage door")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "garage door"
    }

    "parse 'check downstairs hallway lights'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("check downstairs hallway lights")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "downstairs hallway lights"
    }

    "parse 'check the downstairs hallway lights'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("check the downstairs hallway lights")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "downstairs hallway lights"
    }

    "parse 'whats the status of bedroom light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("whats the status of bedroom light")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "bedroom light"
    }

    "parse 'get front door'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("get front door")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "front door"
    }

    "parse 'what is porch light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("what is porch light")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "porch light"
    }

    "parse 'switch the living room light on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("switch the living room light on")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "living room light"
    }

    "parse 'turn garage door off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn garage door off")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "garage door"
    }

    "parse 'switch the fan off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("switch the fan off")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "fan"
    }

    "parse 'turn office light toggle'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("turn office light toggle")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateToggle>()
        val setState = inputData as Sentences.HomeAssistant.SetStateToggle
        setState.entityName?.trim() shouldBe "office light"
    }

    "parse 'switch the basement lights toggle'" {
        val data = Sentences.HomeAssistant["en"]!!
        val (score, inputData) = data.score("switch the basement lights toggle")
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateToggle>()
        val setState = inputData as Sentences.HomeAssistant.SetStateToggle
        setState.entityName?.trim() shouldBe "basement lights"
    }
})

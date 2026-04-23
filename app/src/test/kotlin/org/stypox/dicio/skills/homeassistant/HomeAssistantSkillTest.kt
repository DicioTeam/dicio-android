package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.stypox.dicio.sentences.Sentences

class HomeAssistantSkillTest : StringSpec({
    "parse 'turn outside lights off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn outside lights off"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "outside lights"
    }

    "parse 'turn the kitchen light on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn the kitchen light on"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "kitchen light"
    }

    "parse 'switch bedroom lamp off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "switch bedroom lamp off"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "bedroom lamp"
    }

    "parse 'get status of living room light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "get status of living room light"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "living room light"
    }

    "parse 'what is the status for garage door'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "what is the status for garage door"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "garage door"
    }

    "parse 'check downstairs hallway lights'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "check downstairs hallway lights"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "downstairs hallway lights"
    }

    "parse 'check the downstairs hallway lights'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "check the downstairs hallway lights"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "downstairs hallway lights"
    }

    "parse 'whats the status of bedroom light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "whats the status of bedroom light"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "bedroom light"
    }

    "parse 'get front door'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "get front door"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "front door"
    }

    "parse 'what is porch light'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "what is porch light"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetStatus>()
        val getStatus = inputData as Sentences.HomeAssistant.GetStatus
        getStatus.entityName?.trim() shouldBe "porch light"
    }

    "parse 'switch the living room light on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "switch the living room light on"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "living room light"
    }

    "parse 'turn garage door off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn garage door off"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "garage door"
    }

    "parse 'switch the fan off'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "switch the fan off"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOff>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOff
        setState.entityName?.trim() shouldBe "fan"
    }

    "parse 'turn office light toggle'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn office light toggle"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateToggle>()
        val setState = inputData as Sentences.HomeAssistant.SetStateToggle
        setState.entityName?.trim() shouldBe "office light"
    }

    "parse 'switch the basement lights toggle'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "switch the basement lights toggle"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateToggle>()
        val setState = inputData as Sentences.HomeAssistant.SetStateToggle
        setState.entityName?.trim() shouldBe "basement lights"
    }

    "parse 'where is the person Mark'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "where is the person Mark"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetPersonLocation>()
        val getLocation = inputData as Sentences.HomeAssistant.GetPersonLocation
        getLocation.personName?.trim() shouldBe "Mark"
    }

    "parse 'wheres the person Sarah'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "wheres the person Sarah"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetPersonLocation>()
        val getLocation = inputData as Sentences.HomeAssistant.GetPersonLocation
        getLocation.personName?.trim() shouldBe "Sarah"
    }

    "parse 'whats John location'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "whats John location"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetPersonLocation>()
        val getLocation = inputData as Sentences.HomeAssistant.GetPersonLocation
        getLocation.personName?.trim() shouldBe "John"
    }

    "parse 'what is Emily location'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "what is Emily location"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.GetPersonLocation>()
        val getLocation = inputData as Sentences.HomeAssistant.GetPersonLocation
        getLocation.personName?.trim() shouldBe "Emily"
    }

    // Select Source sentence recognition tests
    "parse 'turn kitchen radio to BBC Radio 2'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn kitchen radio to BBC Radio 2"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "BBC Radio 2"
    }

    "parse 'set kitchen radio on Virgin Radio'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "set kitchen radio on Virgin Radio"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "kitchen radio"
        selectSource.sourceName?.trim() shouldBe "Virgin Radio"
    }

    "parse 'tune the bedroom speaker to Heart Dorset'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "tune the bedroom speaker to Heart Dorset"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "bedroom speaker"
        selectSource.sourceName?.trim() shouldBe "Heart Dorset"
    }

    "parse 'change living room tv to HDMI 1'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "change living room tv to HDMI 1"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SelectSource>()
        val selectSource = inputData as Sentences.HomeAssistant.SelectSource
        selectSource.entityName?.trim() shouldBe "living room tv"
        selectSource.sourceName?.trim() shouldBe "HDMI 1"
    }

    "does not conflict with set_state_on" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn kitchen radio on"
        val (score, inputData) = data.score(TestSkillContext(input), input)
        
        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
        val setState = inputData as Sentences.HomeAssistant.SetStateOn
        setState.entityName?.trim() shouldBe "kitchen radio"
    }
})

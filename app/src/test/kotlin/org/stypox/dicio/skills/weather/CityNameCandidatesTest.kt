package org.stypox.dicio.skills.weather

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.stypox.dicio.skills.weather.WeatherSkill.Companion.cityNameCandidates

class CityNameCandidatesTest : StringSpec({
    "other languages are left untouched" {
        cityNameCandidates("London", "en") shouldBe listOf("London")
    }

    "the original name is always tried first" {
        cityNameCandidates("москва", "ru").first() shouldBe "москва"
        cityNameCandidates("москве", "ru").first() shouldBe "москве"
    }

    "prepositional case is turned into nominative" {
        cityNameCandidates("москве", "ru") shouldContain "москва"
        cityNameCandidates("новосибирске", "ru") shouldContain "новосибирск"
        cityNameCandidates("киеве", "ru") shouldContain "киев"
        cityNameCandidates("твери", "ru") shouldContain "тверь"
        cityNameCandidates("электростали", "ru") shouldContain "электросталь"
        cityNameCandidates("германии", "ru") shouldContain "германия"
        cityNameCandidates("химках", "ru") shouldContain "химки"
        cityNameCandidates("нижнем новгороде", "ru") shouldContain "нижний новгород"
    }
})

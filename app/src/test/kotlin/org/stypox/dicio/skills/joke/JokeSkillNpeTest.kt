package org.stypox.dicio.skills.joke

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.dicio.skill.context.SkillContext
import org.dicio.skill.context.SpeechOutputDevice
import org.dicio.skill.skill.SkillOutput
import org.dicio.numbers.ParserFormatter
import org.dicio.skill.standard.util.MatchHelper
import org.stypox.dicio.sentences.Sentences
import java.util.Locale
import android.content.Context

/**
 * Regression test for https://github.com/DicioTeam/dicio-android/pull/412
 *
 * When the user changes language, there is a race condition where JokeSkill.generateOutput()
 * can be called with a locale not in JOKE_SUPPORTED_LOCALES. Previously this caused a NPE
 * due to !! on resolveSupportedLocale(). The fix returns JokeOutput.Failed instead.
 */
class JokeSkillNpeTest : StringSpec({

    "generateOutput returns Failed when ctx locale is not in JOKE_SUPPORTED_LOCALES" {
        val data = Sentences.Joke["en"]!!
        val skill = JokeSkill(JokeInfo, data)

        val ctx = object : SkillContext {
            override val locale: Locale = Locale.ITALIAN
            override val android: Context get() = throw NotImplementedError()
            override val sentencesLanguage: String = "it"
            override val parserFormatter: ParserFormatter? = null
            override val speechOutputDevice: SpeechOutputDevice get() = throw NotImplementedError()
            override val previousOutput: SkillOutput? = null
            override val standardMatchHelper: MatchHelper? = null
        }

        val result = runBlocking {
            skill.generateOutput(ctx, Sentences.Joke.Command)
        }
        result.shouldBeInstanceOf<JokeOutput.Failed>()
    }

    "generateOutput returns Failed when locale is Turkish" {
        val data = Sentences.Joke["en"]!!
        val skill = JokeSkill(JokeInfo, data)

        val ctx = object : SkillContext {
            override val locale: Locale = Locale("tr")
            override val android: Context get() = throw NotImplementedError()
            override val sentencesLanguage: String = "tr"
            override val parserFormatter: ParserFormatter? = null
            override val speechOutputDevice: SpeechOutputDevice get() = throw NotImplementedError()
            override val previousOutput: SkillOutput? = null
            override val standardMatchHelper: MatchHelper? = null
        }

        val result = runBlocking {
            skill.generateOutput(ctx, Sentences.Joke.Command)
        }
        result.shouldBeInstanceOf<JokeOutput.Failed>()
    }
})

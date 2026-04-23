package org.stypox.dicio.skills.joke

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.json.JSONObject
import org.stypox.dicio.sentences.Sentences.Joke
import org.stypox.dicio.util.ConnectionUtils
import org.stypox.dicio.util.LocaleUtils

class JokeSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Joke>)
    : StandardRecognizerSkill<Joke>(correspondingSkillInfo, data) {
    override suspend fun generateOutput(ctx: SkillContext, inputData: Joke): SkillOutput {
        // we can use !! because the JokeInfo would have declared this skill unavailable
        // if the current locale was not among the supported ones
        val locale = LocaleUtils.resolveSupportedLocale(ctx.locale, JOKE_SUPPORTED_LOCALES)!!

        if (locale == "en") {
            val joke: JSONObject = ConnectionUtils.getPageJson(RANDOM_JOKE_URL_EN)
            return JokeOutput.Success(
                setup = joke.getString("setup"),
                delivery = joke.getString("punchline")
            )
        // Hungarian API uses "title" / "text" instead of "setup" / "delivery"
        } else if (locale == "hu") {
            val joke: JSONObject = ConnectionUtils.getPageJson(RANDOM_JOKE_URL_HU)
            return JokeOutput.Success(
                setup = joke.getString("title"),
                delivery = joke.getString("text")
            )
        } else {
            val joke: JSONObject = ConnectionUtils.getPageJson(
                "$RANDOM_JOKE_URL?lang=$locale&safe-mode&type=twopart"
            )
            return JokeOutput.Success(
                setup = joke.getString("setup"),
                delivery = joke.getString("delivery")
            )
        }
    }

    companion object {
        private const val RANDOM_JOKE_URL = "https://v2.jokeapi.dev/joke/Any"
        private const val RANDOM_JOKE_URL_EN = "https://official-joke-api.appspot.com/random_joke"
        private const val RANDOM_JOKE_URL_HU = "https://viccgyujt-api.anoim.workers.dev/random-vicc"
        val JOKE_SUPPORTED_LOCALES = listOf(
            "cs", "de", "en", "es", "fr", "hu", "pt"
        )
    }
}

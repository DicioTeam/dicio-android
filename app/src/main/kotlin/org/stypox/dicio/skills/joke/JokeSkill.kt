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
        val locale = LocaleUtils.resolveSupportedLocale(ctx.locale, JOKE_SUPPORTED_LOCALES)
            ?: return JokeOutput.Failed()

        if (locale == "en") {
            val joke: JSONObject = ConnectionUtils.getPageJson(RANDOM_JOKE_URL_EN)
            return JokeOutput.Success(
                setup = joke.getString("setup"),
                delivery = joke.getString("punchline")
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
        val JOKE_SUPPORTED_LOCALES = listOf(
            "cs", "de", "en", "es", "fr", "pt"
        )
    }
}

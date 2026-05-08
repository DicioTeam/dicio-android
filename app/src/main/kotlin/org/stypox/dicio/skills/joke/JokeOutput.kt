package org.stypox.dicio.skills.joke

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Subtitle
import org.stypox.dicio.util.getString

sealed interface JokeOutput : SkillOutput {
    data class Success(
        val setup: String,
        val delivery: String,
    ) : JokeOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            // ensure there is a point between the sentences, so that the TTS makes a pause
            if (setup.matches(ENDS_WITH_PUNCTUATION_REGEX))
                "$setup $delivery"
            else
                "$setup. $delivery"

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subtitle(text = setup)
                Body(text = delivery)
            }
        }

        companion object {
            val ENDS_WITH_PUNCTUATION_REGEX = ".*\\p{Punct}$".toRegex()
        }
    }

    class Failed : JokeOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_joke_failed)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Body(text = getSpeechOutput(ctx))
        }
    }
}
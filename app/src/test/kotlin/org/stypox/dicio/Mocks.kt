package org.stypox.dicio

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.dicio.numbers.ParserFormatter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.context.SpeechOutputDevice
import org.dicio.skill.skill.FloatScore
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity
import java.util.Locale
import org.dicio.skill.standard.util.MatchHelper

object MockSkillContext : SkillContext {
    override val android: Context get() = mocked()
    override val locale: Locale get() = mocked()
    override val sentencesLanguage: String get() = mocked()
    override val parserFormatter: ParserFormatter get() = mocked()
    override val speechOutputDevice: SpeechOutputDevice get() = mocked()
    override val previousOutput: SkillOutput get() = mocked()
    override val standardMatchHelper: MatchHelper get() = mocked()
}

object MockSkillInfo : SkillInfo("") {
    override fun name(context: Context): String = mocked()
    override fun sentenceExample(context: Context): String = mocked()
    @Composable override fun icon(): Painter = mocked()
    override fun isAvailable(ctx: SkillContext) = mocked()
    override fun build(ctx: SkillContext) = mocked()
}

class MockSkill(specificity: Specificity, private val score: Float) :
    Skill<Nothing?>(MockSkillInfo, specificity)
{
    var scoreCalled = false
        private set

    override fun score(
        ctx: SkillContext,
        input: String
    ): Pair<Score, Nothing?> {
        scoreCalled = true
        return Pair(FloatScore(score), null)
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Nothing?) = mocked()
}

fun mocked(): Nothing {
    throw NotImplementedError()
}

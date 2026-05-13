package org.stypox.dicio.skills.calculator

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object CalculatorInfo : SkillInfo("calculator") {
    override val categoryNameRes = R.string.category_utilities

    override fun name(context: Context) =
        context.getString(R.string.skill_name_calculator)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_calculator)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Calculate)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Calculator[ctx.sentencesLanguage] != null &&
                Sentences.CalculatorOperators[ctx.sentencesLanguage] != null &&
                ctx.parserFormatter != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return CalculatorSkill(CalculatorInfo, Sentences.Calculator[ctx.sentencesLanguage]!!)
    }
}

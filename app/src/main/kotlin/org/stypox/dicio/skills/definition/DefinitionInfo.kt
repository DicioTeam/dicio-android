package org.stypox.dicio.skills.definition

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object DefinitionInfo : SkillInfo("definition") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_definition)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_definition)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.MenuBook)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Definition[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return DefinitionSkill(DefinitionInfo, Sentences.Definition[ctx.sentencesLanguage]!!)
    }
}

package org.stypox.dicio.skills.cancel

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object CancelInfo : SkillInfo("cancel") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_cancel)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_cancel)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Cancel)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Cancel[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return CancelSkill(CancelInfo, Sentences.Cancel[ctx.sentencesLanguage]!!)
    }
}

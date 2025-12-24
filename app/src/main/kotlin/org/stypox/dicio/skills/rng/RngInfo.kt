package org.stypox.dicio.skills.rng

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object RngInfo : SkillInfo("rng") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_rng)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_rng)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Casino)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Rng[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return RngSkill(RngInfo, Sentences.Rng[ctx.sentencesLanguage]!!)
    }
}

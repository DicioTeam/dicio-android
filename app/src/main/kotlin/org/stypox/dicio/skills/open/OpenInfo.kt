package org.stypox.dicio.skills.open

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.fragment.app.Fragment
import org.dicio.skill.skill.Skill
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

object OpenInfo : SkillInfo("open") {
    override val categoryNameRes = R.string.category_utilities

    override fun name(context: Context) =
        context.getString(R.string.skill_name_open)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_open)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.AutoMirrored.Filled.OpenInNew)

    override fun isAvailable(ctx: SkillContext): Boolean {
        return Sentences.Open[ctx.sentencesLanguage] != null
    }

    override fun build(ctx: SkillContext): Skill<*> {
        return OpenSkill(OpenInfo, Sentences.Open[ctx.sentencesLanguage]!!)
    }
}

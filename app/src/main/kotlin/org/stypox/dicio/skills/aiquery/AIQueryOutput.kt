package org.stypox.dicio.skills.aiquery

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.Subtitle
import org.stypox.dicio.util.getString

class AIQueryOutput(
    private val response: String?,
    private val question: String,
) : SkillOutput {
    
    override fun getSpeechOutput(ctx: SkillContext) = if (response == null) {
        ctx.getString(R.string.skill_aiquery_error)
    } else {
        response
    }

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        if (response == null) {
            Headline(text = getSpeechOutput(ctx))
        } else {
            Column {
                Subtitle(text = question)
                Headline(text = response)
            }
        }
    }
}
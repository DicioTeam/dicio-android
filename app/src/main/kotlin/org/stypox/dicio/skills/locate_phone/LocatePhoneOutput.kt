package org.stypox.dicio.skills.locate_phone


import androidx.compose.runtime.Composable
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.util.getString
import org.stypox.dicio.R

sealed interface LocatePhoneOutput: SkillOutput {
    data class CallOut(val unused: String) : LocatePhoneOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_sentence_locate_phone)

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Headline(text = getSpeechOutput(ctx))
        }
    }
}
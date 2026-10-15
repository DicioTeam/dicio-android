package org.stypox.dicio.skills.where_are_you

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput

data class LocatePhoneOutput : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
    context.getString(R.string.skill_sentence_locate_phone)
}
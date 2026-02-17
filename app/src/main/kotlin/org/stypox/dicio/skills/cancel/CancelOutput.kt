package org.stypox.dicio.skills.cancel

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.InteractionPlan
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.getString

class CancelOutput : HeadlineSpeechSkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String =
        ctx.getString(R.string.skill_cancel_confirmation)

    override fun getInteractionPlan(ctx: SkillContext): InteractionPlan =
        InteractionPlan.FinishInteraction
}

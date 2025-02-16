package org.stypox.dicio.skills.checklist

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput

/**
 * A [SkillOutput] where the graphical output is just a headline text with the speech output.
 */
open class ChecklistSkillOutput(private val literal: String, private val interactionPlan: InteractionPlan) : HeadlineSpeechSkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = literal

    override fun getInteractionPlan(ctx: SkillContext): InteractionPlan =
        interactionPlan

    fun updateText(update: (String) -> String): ChecklistSkillOutput =
        ChecklistSkillOutput(update(literal), interactionPlan)
}
